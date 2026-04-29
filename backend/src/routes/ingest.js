import { Router } from 'express';
import { asyncRoute, requireApiKey } from '../lib/http.js';
import { ingestMetrics, rebuildDailySummaries } from '../services/metrics.js';

export const ingestRouter = Router();

ingestRouter.post(
  '/health-connect',
  requireApiKey,
  asyncRoute(async (req, res) => {
    const result = await ingestMetrics(req.body);
    await rebuildDailySummaries({
      from: result.metricDates.sort()[0],
      to: result.metricDates.sort().at(-1)
    });
    res.status(202).json({ ok: true, ...result });
  })
);
