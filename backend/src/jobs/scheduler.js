import cron from 'node-cron';
import { config } from '../lib/config.js';
import { rebuildDailySummaries } from '../services/metrics.js';
import { generateWeeklyReport } from '../services/reports.js';

export function startScheduler() {
  if (!config.runCron) {
    return [];
  }

  const tasks = [];

  tasks.push(
    cron.schedule(
      config.dailyCron,
      async () => {
        try {
          const result = await rebuildDailySummaries();
          console.log('[cron] daily summary rebuilt', result);
        } catch (error) {
          console.error('[cron] daily summary failed', error);
        }
      },
      { timezone: config.timezone }
    )
  );

  tasks.push(
    cron.schedule(
      config.weeklyReportCron,
      async () => {
        try {
          const report = await generateWeeklyReport();
          console.log('[cron] weekly report generated', {
            weekStart: report.week_start,
            weekEnd: report.week_end
          });
        } catch (error) {
          console.error('[cron] weekly report failed', error);
        }
      },
      { timezone: config.timezone }
    )
  );

  return tasks;
}
