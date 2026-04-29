export function MetricTile({ icon, label, value, subValue, delta, tone = 'neutral' }) {
  return (
    <section className={`metric-tile metric-tile--${tone}`}>
      <div className="metric-tile__top">
        <div className="metric-tile__icon" aria-hidden="true">
          {icon}
        </div>
        <p className="metric-tile__label">{label}</p>
      </div>
      <div className="metric-tile__content">
        <p className="metric-tile__value">{value}</p>
        {delta ? <p className="metric-tile__delta">{delta}</p> : null}
        {subValue ? <p className="metric-tile__sub">{subValue}</p> : null}
      </div>
    </section>
  );
}
