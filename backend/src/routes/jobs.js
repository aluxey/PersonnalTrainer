import { Router } from 'express';
import { asyncRoute, requireApiKey } from '../lib/http.js';
import { rebuildDailySummaries } from '../services/metrics.js';
import { generateWeeklyReport } from '../services/reports.js';

export const jobsRouter = Router();

jobsRouter.post(
  '/daily-summary',
  requireApiKey,
  asyncRoute(async (req, res) => {
    const result = await rebuildDailySummaries(req.body || {});
    res.json({ ok: true, ...result });
  })
);

jobsRouter.post(
  '/weekly-report',
  requireApiKey,
  asyncRoute(async (req, res) => {
    const report = await generateWeeklyReport(req.body || {});
    res.json({ ok: true, report });
  })
);
