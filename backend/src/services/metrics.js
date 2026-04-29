import crypto from 'node:crypto';
import { supabase } from '../lib/supabase.js';

const SUM_METRICS = new Set([
  'steps',
  'distance_m',
  'sleep_duration_min',
  'sleep_deep_min',
  'sleep_rem_min',
  'sleep_awake_min',
  'active_energy_kcal',
  'workout_duration_min',
  'workout_energy_kcal',
  'calories_intake_kcal',
  'protein_g',
  'carbs_g',
  'fat_g',
  'fiber_g',
  'water_ml'
]);

const AVG_METRICS = new Set([
  'sleep_score',
  'heart_rate_resting_bpm',
  'weight_kg',
  'body_fat_pct'
]);

const DEFAULT_UNITS = {
  steps: 'count',
  distance_m: 'm',
  sleep_duration_min: 'min',
  sleep_deep_min: 'min',
  sleep_rem_min: 'min',
  sleep_awake_min: 'min',
  sleep_score: 'score',
  active_energy_kcal: 'kcal',
  workout_duration_min: 'min',
  workout_energy_kcal: 'kcal',
  heart_rate_resting_bpm: 'bpm',
  weight_kg: 'kg',
  body_fat_pct: '%',
  calories_intake_kcal: 'kcal',
  protein_g: 'g',
  carbs_g: 'g',
  fat_g: 'g',
  fiber_g: 'g',
  water_ml: 'ml'
};

function isIsoDate(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function makeDedupeKey(row) {
  const stable = [
    row.metric_date,
    row.source,
    row.metric,
    row.start_time || '',
    row.end_time || ''
  ].join('|');
  return crypto.createHash('sha256').update(stable).digest('hex');
}

export function normalizePayload(payload) {
  if (!payload || typeof payload !== 'object') {
    throw Object.assign(new Error('Body must be a JSON object'), { statusCode: 400 });
  }

  const date = payload.date || payload.metric_date;
  if (!isIsoDate(date)) {
    throw Object.assign(new Error('date must use YYYY-MM-DD'), { statusCode: 400 });
  }

  const source = String(payload.source || 'health_connect').trim().toLowerCase();
  if (!Array.isArray(payload.metrics) || payload.metrics.length === 0) {
    throw Object.assign(new Error('metrics must be a non-empty array'), { statusCode: 400 });
  }

  return payload.metrics.map((entry) => {
    const metric = String(entry.metric || '').trim();
    const value = toNumber(entry.value);
    if (!metric || value === null) {
      throw Object.assign(new Error('Each metric requires metric and numeric value'), { statusCode: 400 });
    }

    const row = {
      metric_date: date,
      source: String(entry.source || source).trim().toLowerCase(),
      metric,
      value,
      unit: String(entry.unit || DEFAULT_UNITS[metric] || '').trim() || 'unknown',
      start_time: entry.start_time || null,
      end_time: entry.end_time || null,
      metadata: entry.metadata && typeof entry.metadata === 'object' ? entry.metadata : {}
    };
    row.dedupe_key = makeDedupeKey(row);
    return row;
  });
}

export async function ingestMetrics(payload) {
  const rows = normalizePayload(payload);
  const { error, count } = await supabase
    .from('daily_metric_entries')
    .upsert(rows, { onConflict: 'dedupe_key', count: 'exact' });

  if (error) {
    throw error;
  }

  return {
    insertedOrUpdated: count ?? rows.length,
    metricDates: [...new Set(rows.map((row) => row.metric_date))]
  };
}

function reduceMetric(metric, values) {
  if (AVG_METRICS.has(metric)) {
    return values.reduce((sum, value) => sum + value, 0) / values.length;
  }
  if (SUM_METRICS.has(metric)) {
    return values.reduce((sum, value) => sum + value, 0);
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function scoreDay(metrics) {
  const score = {};
  if (typeof metrics.steps === 'number') {
    score.steps_ok = metrics.steps >= 9000;
  }
  if (typeof metrics.sleep_duration_min === 'number') {
    score.sleep_ok = metrics.sleep_duration_min >= 450;
  }
  if (typeof metrics.protein_g === 'number') {
    score.protein_ok = metrics.protein_g >= 150;
  }
  return score;
}

export function summarizeEntries(entries) {
  const grouped = new Map();
  for (const entry of entries) {
    if (!grouped.has(entry.metric_date)) {
      grouped.set(entry.metric_date, new Map());
    }
    const byMetric = grouped.get(entry.metric_date);
    if (!byMetric.has(entry.metric)) {
      byMetric.set(entry.metric, []);
    }
    byMetric.get(entry.metric).push(Number(entry.value));
  }

  return [...grouped.entries()].map(([metricDate, byMetric]) => {
    const metrics = {};
    for (const [metric, values] of byMetric.entries()) {
      metrics[metric] = Number(reduceMetric(metric, values).toFixed(2));
    }
    return {
      metric_date: metricDate,
      metrics,
      score: scoreDay(metrics)
    };
  });
}

export async function rebuildDailySummaries({ from, to } = {}) {
  let query = supabase
    .from('daily_metric_entries')
    .select('metric_date,metric,value')
    .order('metric_date', { ascending: true });

  if (from) {
    query = query.gte('metric_date', from);
  }
  if (to) {
    query = query.lte('metric_date', to);
  }

  const { data, error } = await query;
  if (error) {
    throw error;
  }

  const summaries = summarizeEntries(data || []);
  if (summaries.length === 0) {
    return { upserted: 0 };
  }

  const { error: upsertError } = await supabase
    .from('daily_summaries')
    .upsert(summaries, { onConflict: 'metric_date' });

  if (upsertError) {
    throw upsertError;
  }

  return { upserted: summaries.length };
}

export async function fetchDailySummaries({ from, to } = {}) {
  let query = supabase
    .from('daily_summaries')
    .select('metric_date,metrics,score')
    .order('metric_date', { ascending: true });

  if (from) {
    query = query.gte('metric_date', from);
  }
  if (to) {
    query = query.lte('metric_date', to);
  }

  const { data, error } = await query;
  if (error) {
    throw error;
  }
  return data || [];
}
