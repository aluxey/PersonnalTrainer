import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  Bed,
  Bike,
  CalendarDays,
  ChartNoAxesCombined,
  CheckCircle2,
  ClipboardList,
  Dumbbell,
  Flame,
  Footprints,
  Home,
  Moon,
  Settings,
  RefreshCw,
  Scale,
  Sparkles,
  TrendingDown,
  Utensils,
  Waves
} from 'lucide-react';
import {
  Bar,
  ComposedChart,
  Line,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { supabase } from './lib/supabase.js';
import { MetricTile } from './components/MetricTile.jsx';

const DAILY_TARGETS = {
  calories: 2200,
  protein: 160,
  steps: 10000,
  water: 2500,
  sleep: 450
};

function formatNumber(value, suffix = '') {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-';
  }
  return `${Number(value).toLocaleString('fr-FR', { maximumFractionDigits: 1 })}${suffix}`;
}

function formatCompact(value, suffix = '') {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-';
  }
  return `${Math.round(Number(value)).toLocaleString('fr-FR')}${suffix}`;
}

function formatSleep(minutes) {
  if (minutes === null || minutes === undefined || Number.isNaN(minutes)) {
    return '-';
  }
  const rounded = Math.round(Number(minutes));
  const hours = Math.floor(rounded / 60);
  const rest = rounded % 60;
  return `${hours} h ${String(rest).padStart(2, '0')} min`;
}

function percent(value, target) {
  if (!value || !target) {
    return 0;
  }
  return Math.min(100, Math.round((Number(value) / Number(target)) * 100));
}

function normalizeSummary(row) {
  return {
    date: row.metric_date,
    ...row.metrics
  };
}

function shortDate(value) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('fr-FR', {
    day: 'numeric',
    month: 'short'
  }).format(new Date(`${value}T12:00:00`));
}

function average(rows, key) {
  const values = rows
    .map((row) => row[key])
    .filter((value) => typeof value === 'number');
  if (values.length === 0) {
    return null;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function delta(current, previous) {
  if (typeof current !== 'number' || typeof previous !== 'number') {
    return null;
  }
  return current - previous;
}

function GoalRow({ icon, label, value, target, unit, tone, formatter = formatCompact }) {
  const completion = percent(value, target);

  return (
    <div className="goal-row">
      <div className={`goal-row__icon goal-row__icon--${tone}`}>{icon}</div>
      <div className="goal-row__main">
        <div className="goal-row__meta">
          <span>{label}</span>
          <strong>
            {formatter(value)}
            {' / '}
            {formatter(target)}
            {unit}
          </strong>
        </div>
        <div className="goal-row__track">
          <span className={`goal-row__bar goal-row__bar--${tone}`} style={{ width: `${completion}%` }} />
        </div>
      </div>
      <span className="goal-row__percent">{completion} %</span>
    </div>
  );
}

export default function App() {
  const [summaries, setSummaries] = useState([]);
  const [weeklyReport, setWeeklyReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function loadData() {
    setLoading(true);
    setError(null);

    const since = new Date();
    since.setDate(since.getDate() - 45);
    const sinceDate = since.toISOString().slice(0, 10);

    const [summaryResult, reportResult] = await Promise.all([
      supabase
        .from('daily_summaries')
        .select('metric_date,metrics,score')
        .gte('metric_date', sinceDate)
        .order('metric_date', { ascending: true }),
      supabase
        .from('weekly_reports')
        .select('week_start,week_end,metrics,insights,markdown')
        .order('week_start', { ascending: false })
        .limit(1)
        .maybeSingle()
    ]);

    if (summaryResult.error) {
      throw summaryResult.error;
    }
    if (reportResult.error) {
      throw reportResult.error;
    }

    setSummaries((summaryResult.data || []).map(normalizeSummary));
    setWeeklyReport(reportResult.data || null);
    setLoading(false);
  }

  useEffect(() => {
    loadData().catch((loadError) => {
      setError(loadError.message);
      setLoading(false);
    });
  }, []);

  const latest = summaries.at(-1) || {};
  const trendRows = useMemo(() => summaries.slice(-30), [summaries]);
  const weekRows = useMemo(() => summaries.slice(-7), [summaries]);
  const previousWeekRows = useMemo(() => summaries.slice(-14, -7), [summaries]);

  const weeklyAverages = useMemo(() => {
    return {
      steps: average(weekRows, 'steps'),
      sleep: average(weekRows, 'sleep_duration_min'),
      calories: average(weekRows, 'calories_intake_kcal'),
      protein: average(weekRows, 'protein_g'),
      weight: average(weekRows, 'weight_kg'),
      activeEnergy: average(weekRows, 'active_energy_kcal')
    };
  }, [weekRows]);

  const previousWeeklyAverages = useMemo(() => ({
    weight: average(previousWeekRows, 'weight_kg'),
    calories: average(previousWeekRows, 'calories_intake_kcal'),
    steps: average(previousWeekRows, 'steps')
  }), [previousWeekRows]);

  const chartRows = useMemo(() => trendRows.map((row) => ({
    ...row,
    label: shortDate(row.date),
    sleep_hours: typeof row.sleep_duration_min === 'number' ? Number((row.sleep_duration_min / 60).toFixed(2)) : null
  })), [trendRows]);

  const weightDelta = delta(weeklyAverages.weight, previousWeeklyAverages.weight);
  const proteinPct = percent(latest.protein_g, DAILY_TARGETS.protein);
  const carbs = latest.carbs_g || 0;
  const protein = latest.protein_g || 0;
  const fat = latest.fat_g || 0;
  const totalMacros = protein + carbs + fat;
  const proteinShare = totalMacros ? Math.round((protein / totalMacros) * 100) : 0;
  const carbsShare = totalMacros ? Math.round((carbs / totalMacros) * 100) : 0;
  const fatShare = totalMacros ? Math.round((fat / totalMacros) * 100) : 0;
  const sleepScore = latest.sleep_score || percent(latest.sleep_duration_min, DAILY_TARGETS.sleep);
  const latestWorkout = latest.workout_duration_min || 0;
  const latestEnergy = latest.workout_energy_kcal || latest.active_energy_kcal || 0;
  const latestDistance = latest.distance_m ? `${formatNumber(latest.distance_m / 1000, ' km')}` : 'Distance a connecter';

  return (
    <div className="dashboard-layout">
      <aside className="sidebar" aria-label="Navigation principale">
        <div className="sidebar__brand"><Home size={22} /></div>
        <nav className="sidebar__nav">
          <a className="sidebar__item sidebar__item--active" href="#overview" title="Vue generale"><ChartNoAxesCombined size={21} /></a>
          <a className="sidebar__item" href="#objectives" title="Objectifs"><CalendarDays size={21} /></a>
          <a className="sidebar__item" href="#training" title="Activite"><Dumbbell size={21} /></a>
          <a className="sidebar__item" href="#nutrition" title="Nutrition"><Utensils size={21} /></a>
          <a className="sidebar__item" href="#sleep" title="Sommeil"><Moon size={21} /></a>
          <a className="sidebar__item" href="#report" title="Rapport"><ClipboardList size={21} /></a>
          <a className="sidebar__item" href="#settings" title="Reglages"><Settings size={21} /></a>
        </nav>
        <div className="sidebar__avatar">PT</div>
      </aside>

      <main className="app-shell" id="overview">
        <header className="topbar">
          <div>
            <p className="eyebrow">Health Connect + Supabase</p>
            <h1>Personal Trainer</h1>
            <p className="topbar__sub">Pilotage sommeil, activite, nutrition et perte de poids.</p>
          </div>
          <button className="icon-button" type="button" onClick={() => loadData()} disabled={loading} title="Rafraichir">
            <RefreshCw size={18} />
            <span>Rafraichir</span>
          </button>
        </header>

        {error ? <p className="alert">Erreur : {error}</p> : null}

        <section className="metric-grid" aria-label="Dernieres mesures">
          <MetricTile
            icon={<Scale size={22} />}
            label="Poids actuel"
            value={formatNumber(latest.weight_kg, ' kg')}
            delta={weightDelta !== null ? `${weightDelta <= 0 ? 'Baisse' : 'Hausse'} ${formatNumber(Math.abs(weightDelta), ' kg')}` : null}
            subValue={`Moy. 7 j : ${formatNumber(weeklyAverages.weight, ' kg')}`}
            tone="body"
          />
          <MetricTile
            icon={<Utensils size={22} />}
            label="Calories consommees"
            value={formatCompact(latest.calories_intake_kcal, ' kcal')}
            subValue={`Moy. 7 j : ${formatCompact(weeklyAverages.calories, ' kcal')}`}
            tone="food"
          />
          <MetricTile
            icon={<Footprints size={22} />}
            label="Pas"
            value={formatCompact(latest.steps)}
            subValue={`Moy. 7 j : ${formatCompact(weeklyAverages.steps)}`}
            tone="activity"
          />
          <MetricTile
            icon={<Bed size={22} />}
            label="Sommeil"
            value={formatSleep(latest.sleep_duration_min)}
            subValue={`Moy. 7 j : ${formatSleep(weeklyAverages.sleep)}`}
            tone="sleep"
          />
          <MetricTile
            icon={<Activity size={22} />}
            label="Proteines"
            value={formatCompact(latest.protein_g, ' g')}
            subValue={`Moy. 7 j : ${formatCompact(weeklyAverages.protein, ' g')}`}
            tone="protein"
          />
          <MetricTile
            icon={<Flame size={22} />}
            label="Calories actives"
            value={formatCompact(latest.active_energy_kcal, ' kcal')}
            subValue={`Moy. 7 j : ${formatCompact(weeklyAverages.activeEnergy, ' kcal')}`}
            tone="effort"
          />
        </section>

        <section className="dashboard-grid dashboard-grid--top">
          <div className="panel panel--chart">
            <div className="panel__header">
              <div>
                <h2>Tendances 30 jours</h2>
                <p>Poids, pas, calories et sommeil sur les dernieres donnees synchronisees.</p>
              </div>
              <span>{loading ? 'Chargement' : `${trendRows.length} jours`}</span>
            </div>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={chartRows} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e3e8ef" vertical={false} />
                  <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#607089' }} tickLine={false} axisLine={false} minTickGap={22} />
                  <YAxis yAxisId="left" tick={{ fontSize: 12, fill: '#607089' }} tickLine={false} axisLine={false} width={38} />
                  <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12, fill: '#607089' }} tickLine={false} axisLine={false} width={46} />
                  <Tooltip contentStyle={{ borderRadius: 10, borderColor: '#d9e2ec' }} />
                  <Legend verticalAlign="top" align="right" iconType="circle" wrapperStyle={{ paddingBottom: 14 }} />
                  <Bar yAxisId="right" dataKey="active_energy_kcal" name="Calories actives" fill="#82a9f4" radius={[4, 4, 0, 0]} maxBarSize={16} />
                  <Line yAxisId="left" type="monotone" dataKey="weight_kg" name="Poids kg" stroke="#167982" strokeWidth={3} dot={false} />
                  <Line yAxisId="right" type="monotone" dataKey="steps" name="Pas" stroke="#7c5ce6" strokeWidth={3} dot={false} />
                  <Line yAxisId="left" type="monotone" dataKey="sleep_hours" name="Sommeil h" stroke="#1f82d0" strokeWidth={3} dot={false} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </div>

          <section className="panel objectives-panel" id="objectives">
            <div className="panel__header">
              <div>
                <h2>Objectifs du jour</h2>
                <p>Progression vs cibles configurees.</p>
              </div>
            </div>
            <div className="goal-list">
              <GoalRow icon={<Flame size={19} />} label="Calories" value={latest.calories_intake_kcal} target={DAILY_TARGETS.calories} unit=" kcal" tone="food" />
              <GoalRow icon={<Activity size={19} />} label="Proteines" value={latest.protein_g} target={DAILY_TARGETS.protein} unit=" g" tone="protein" />
              <GoalRow icon={<Footprints size={19} />} label="Pas" value={latest.steps} target={DAILY_TARGETS.steps} unit="" tone="activity" />
              <GoalRow icon={<Waves size={19} />} label="Eau" value={latest.water_ml} target={DAILY_TARGETS.water} unit=" ml" tone="water" />
              <GoalRow icon={<Bed size={19} />} label="Sommeil" value={latest.sleep_duration_min} target={DAILY_TARGETS.sleep} unit=" min" tone="sleep" formatter={formatCompact} />
            </div>
          </section>
        </section>

        <section className="dashboard-grid dashboard-grid--cards">
          <section className="panel compact-panel" id="nutrition">
            <div className="panel__header">
              <div>
                <h2>Repartition nutrition</h2>
                <p>Macros du dernier jour.</p>
              </div>
            </div>
            <div className="nutrition-card">
              <div
                className="donut"
                style={{
                  '--protein': `${proteinShare}%`,
                  '--carbs': `${proteinShare + carbsShare}%`
                }}
              >
                <strong>{formatCompact(latest.calories_intake_kcal)}</strong>
                <span>kcal</span>
              </div>
              <div className="macro-list">
                <p><span className="dot dot--protein" />Proteines <strong>{formatCompact(protein, ' g')} ({proteinShare}%)</strong></p>
                <p><span className="dot dot--carbs" />Glucides <strong>{formatCompact(carbs, ' g')} ({carbsShare}%)</strong></p>
                <p><span className="dot dot--fat" />Lipides <strong>{formatCompact(fat, ' g')} ({fatShare}%)</strong></p>
              </div>
            </div>
            <p className="panel-note">Cible utile : proteines hautes, fibres stables, deficit modere.</p>
          </section>

          <section className="panel compact-panel" id="sleep">
            <div className="panel__header">
              <div>
                <h2>Sommeil</h2>
                <p>Derniere nuit synchronisee.</p>
              </div>
            </div>
            <div className="sleep-card">
              <div className="sleep-score" style={{ '--score': `${Math.min(100, Math.round(sleepScore))}%` }}>
                <strong>{Math.round(sleepScore || 0)}</strong>
                <span>score</span>
              </div>
              <div className="sleep-stats">
                <p><span>Duree</span><strong>{formatSleep(latest.sleep_duration_min)}</strong></p>
                <p><span>Profond</span><strong>{formatSleep(latest.sleep_deep_min)}</strong></p>
                <p><span>REM</span><strong>{formatSleep(latest.sleep_rem_min)}</strong></p>
              </div>
            </div>
            <div className="sleep-strip">
              <span className="sleep-strip__deep" />
              <span className="sleep-strip__light" />
              <span className="sleep-strip__rem" />
              <span className="sleep-strip__awake" />
            </div>
          </section>

          <section className="panel compact-panel" id="training">
            <div className="panel__header">
              <div>
                <h2>Activite recente</h2>
                <p>Resume issu des aggregats.</p>
              </div>
              <span>Voir tout</span>
            </div>
            <div className="activity-list">
              <p><span><Dumbbell size={18} /> Entrainement</span><strong>{formatSleep(latestWorkout)}</strong></p>
              <p><span><Flame size={18} /> Energie</span><strong>{formatCompact(latestEnergy, ' kcal')}</strong></p>
              <p><span><Bike size={18} /> Distance</span><strong>{latestDistance}</strong></p>
            </div>
          </section>

          <section className="panel compact-panel">
            <div className="panel__header">
              <div>
                <h2>Analyse intelligente</h2>
                <p>Signaux actionnables.</p>
              </div>
            </div>
            <div className="insight-list insight-list--compact">
              <p className="insight insight--ok"><TrendingDown size={18} /> {weightDelta !== null && weightDelta < 0 ? `Poids moyen en baisse de ${formatNumber(Math.abs(weightDelta), ' kg')}.` : 'Poids a suivre sur 14 jours.'}</p>
              <p className="insight insight--info"><Moon size={18} /> {weeklyAverages.sleep && weeklyAverages.sleep >= 420 ? 'Sommeil compatible avec une bonne recuperation.' : 'Prioriser 7 h de sommeil pour mieux tenir le deficit.'}</p>
              <p className="insight insight--warning"><Flame size={18} /> {proteinPct >= 90 ? 'Proteines proches de la cible.' : 'Proteines sous la cible, risque de faim plus eleve.'}</p>
            </div>
          </section>
        </section>

        <section className="panel report-panel" id="report">
          <div className="report-panel__icon"><ClipboardList size={28} /></div>
          <div className="report-panel__body">
            <div className="panel__header">
              <div>
                <h2>Dernier rapport hebdomadaire</h2>
                <p>{weeklyReport ? `${weeklyReport.week_start} - ${weeklyReport.week_end}` : 'Aucun rapport genere pour le moment'}</p>
              </div>
            </div>
            {weeklyReport ? (
              <div className="insight-list">
                {(weeklyReport.insights || []).map((insight, index) => (
                  <p key={`${insight.type}-${index}`} className={`insight insight--${insight.level}`}>
                    <Sparkles size={18} />
                    {insight.text}
                  </p>
                ))}
              </div>
            ) : (
              <p className="empty-state">Le premier rapport sera genere par le cron hebdomadaire ou via l'endpoint de job.</p>
            )}
          </div>
          <div className="weekly-outcome">
            <CheckCircle2 size={22} />
            <strong>{weightDelta !== null ? `${weightDelta <= 0 ? '-' : '+'}${formatNumber(Math.abs(weightDelta), ' kg')}` : '-'}</strong>
            <span>Evolution poids</span>
          </div>
        </section>
      </main>
    </div>
  );
}
