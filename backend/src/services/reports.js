import { supabase } from '../lib/supabase.js';
import { fetchDailySummaries, rebuildDailySummaries } from './metrics.js';

function dateOnly(date) {
  return date.toISOString().slice(0, 10);
}

function addDays(value, days) {
  const next = new Date(value);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

export function defaultCompletedWeek(today = new Date()) {
  const end = addDays(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()), -1);
  const start = addDays(end, -6);
  return {
    weekStart: dateOnly(start),
    weekEnd: dateOnly(end)
  };
}

function avg(rows, metric) {
  const values = rows
    .map((row) => row.metrics?.[metric])
    .filter((value) => typeof value === 'number');
  if (values.length === 0) {
    return null;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function total(rows, metric) {
  const values = rows
    .map((row) => row.metrics?.[metric])
    .filter((value) => typeof value === 'number');
  if (values.length === 0) {
    return null;
  }
  return values.reduce((sum, value) => sum + value, 0);
}

function fmt(value, unit = '') {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-';
  }
  const rounded = Number(value).toFixed(1).replace(/\.0$/, '');
  return `${rounded}${unit}`;
}

function buildInsights(metrics) {
  const insights = [];
  if (metrics.avg_steps !== null && metrics.avg_steps < 9000) {
    insights.push({
      type: 'activity',
      level: 'warning',
      text: 'Les pas moyens sont sous la cible. Augmenter le NEAT est souvent plus durable que rajouter beaucoup de sport.'
    });
  }
  if (metrics.avg_sleep_duration_min !== null && metrics.avg_sleep_duration_min < 420) {
    insights.push({
      type: 'recovery',
      level: 'warning',
      text: 'Le sommeil moyen est court. La faim, les envies sucrees et la baisse de depense peuvent augmenter.'
    });
  }
  if (metrics.avg_protein_g !== null && metrics.avg_protein_g < 150) {
    insights.push({
      type: 'nutrition',
      level: 'warning',
      text: 'Les proteines moyennes sont sous la cible. C’est un levier prioritaire pour la satiete et la conservation musculaire.'
    });
  }
  if (metrics.avg_calories_intake_kcal !== null && metrics.avg_calories_intake_kcal > 2100) {
    insights.push({
      type: 'nutrition',
      level: 'warning',
      text: 'Les calories moyennes depassent la cible configuree. Regarde d’abord les 1 ou 2 jours qui tirent la moyenne.'
    });
  }
  if (insights.length === 0) {
    insights.push({
      type: 'weekly',
      level: 'ok',
      text: 'Les principaux signaux sont dans les cibles configurees.'
    });
  }
  return insights;
}

function buildMarkdown({ weekStart, weekEnd, metrics, insights }) {
  const lines = [
    '# Rapport hebdomadaire',
    '',
    `Periode : ${weekStart} au ${weekEnd}.`,
    '',
    '## Moyennes',
    '',
    `- Poids moyen : ${fmt(metrics.avg_weight_kg, ' kg')}`,
    `- Calories moyennes : ${fmt(metrics.avg_calories_intake_kcal, ' kcal')}`,
    `- Proteines moyennes : ${fmt(metrics.avg_protein_g, ' g')}`,
    `- Pas moyens : ${fmt(metrics.avg_steps)}`,
    `- Sommeil moyen : ${fmt(metrics.avg_sleep_duration_min, ' min')}`,
    `- Calories actives moyennes : ${fmt(metrics.avg_active_energy_kcal, ' kcal')}`,
    '',
    '## Totaux',
    '',
    `- Sport : ${fmt(metrics.total_workout_duration_min, ' min')}`,
    `- Calories actives : ${fmt(metrics.total_active_energy_kcal, ' kcal')}`,
    '',
    '## Analyse',
    '',
    ...insights.map((insight) => `- ${insight.text}`),
    ''
  ];

  return lines.join('\n');
}

export async function generateWeeklyReport({ weekStart, weekEnd } = {}) {
  const defaults = defaultCompletedWeek();
  const from = weekStart || defaults.weekStart;
  const to = weekEnd || defaults.weekEnd;

  await rebuildDailySummaries({ from, to });
  const rows = await fetchDailySummaries({ from, to });

  const metrics = {
    days_with_data: rows.length,
    avg_weight_kg: avg(rows, 'weight_kg'),
    avg_calories_intake_kcal: avg(rows, 'calories_intake_kcal'),
    avg_protein_g: avg(rows, 'protein_g'),
    avg_steps: avg(rows, 'steps'),
    avg_sleep_duration_min: avg(rows, 'sleep_duration_min'),
    avg_active_energy_kcal: avg(rows, 'active_energy_kcal'),
    total_workout_duration_min: total(rows, 'workout_duration_min'),
    total_active_energy_kcal: total(rows, 'active_energy_kcal')
  };

  const insights = buildInsights(metrics);
  const markdown = buildMarkdown({ weekStart: from, weekEnd: to, metrics, insights });

  const record = {
    week_start: from,
    week_end: to,
    metrics,
    insights,
    markdown
  };

  const { error } = await supabase
    .from('weekly_reports')
    .upsert(record, { onConflict: 'week_start' });

  if (error) {
    throw error;
  }

  return record;
}
