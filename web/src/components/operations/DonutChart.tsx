import { useId, useMemo, useState } from 'react';

import { formatPercent, formatRounded } from '../../operations/format';
import { CategoryIcon } from './Icon';

export interface DonutItem {
  key: string;
  label: string;
  color: string;
  icon: string;
  value: number;
  percent: number;
  share: number;
}

interface DonutChartProps {
  items: DonutItem[];
  currency: string;
  activeKeys: string[];
  emptyText: string;
  ariaLabel: string;
  onSelect: (key: string) => void;
}

interface Segment extends DonutItem {
  start: number;
  end: number;
  mid: number;
}

const SIZE = 380;
const CENTER = SIZE / 2;
const RADIUS = 112;
const THICKNESS = 46;
const LABEL_RADIUS = RADIUS + THICKNESS / 2 + 28;
const TAU = Math.PI * 2;
const START = -Math.PI / 2;

/** Минимальная длина сегмента — чтобы в маленькую категорию помещалась иконка. */
const MIN_SWEEP = (THICKNESS * 1.12) / RADIUS;

function point(angle: number, radius: number): [number, number] {
  return [
    CENTER + radius * Math.cos(angle),
    CENTER + radius * Math.sin(angle),
  ];
}

function arcPath(from: number, to: number): string {
  const [x0, y0] = point(from, RADIUS);
  const [x1, y1] = point(to, RADIUS);
  const large = to - from > Math.PI ? 1 : 0;

  return `M ${x0.toFixed(2)} ${y0.toFixed(2)} A ${RADIUS} ${RADIUS} 0 ${large} 1 ${x1.toFixed(2)} ${y1.toFixed(2)}`;
}

function layoutSweeps(shares: number[]): number[] {
  const count = shares.length;

  if (count === 0) {
    return [];
  }

  if (count * MIN_SWEEP >= TAU) {
    return shares.map(() => TAU / count);
  }

  const locked = shares.map(() => false);

  const measure = () => {
    let lockedSweep = 0;
    let freeShare = 0;

    shares.forEach((share, index) => {
      if (locked[index]) {
        lockedSweep += MIN_SWEEP;
      } else {
        freeShare += share;
      }
    });

    return { lockedSweep, freeShare };
  };

  for (let pass = 0; pass < count; pass += 1) {
    const { lockedSweep, freeShare } = measure();
    let changed = false;

    shares.forEach((share, index) => {
      if (
        !locked[index] &&
        freeShare > 0 &&
        ((TAU - lockedSweep) * share) / freeShare < MIN_SWEEP
      ) {
        locked[index] = true;
        changed = true;
      }
    });

    if (!changed) {
      break;
    }
  }

  const { lockedSweep, freeShare } = measure();

  return shares.map((share, index) =>
    locked[index] || freeShare <= 0
      ? MIN_SWEEP
      : ((TAU - lockedSweep) * share) / freeShare,
  );
}

function DonutChart({
  items,
  currency,
  activeKeys,
  emptyText,
  ariaLabel,
  onSelect,
}: DonutChartProps) {
  const rawId = useId().replace(/[^a-zA-Z0-9_-]/g, '');
  const glowId = `ops-glow-${rawId}`;
  const maskId = `ops-reveal-${rawId}`;

  const [hovered, setHovered] = useState<string | null>(null);

  const segments = useMemo<Segment[]>(() => {
    const sweeps = layoutSweeps(
      items.map((item) => Math.max(0, item.share)),
    );

    return items.map((item, index) => {
      const start =
        START +
        sweeps
          .slice(0, index)
          .reduce((sum, sweep) => sum + sweep, 0);

      const end = start + (sweeps[index] ?? 0);

      return {
        ...item,
        start,
        end,
        mid: (start + end) / 2,
      };
    });
  }, [items]);

  const focusKey =
    hovered ?? (activeKeys.length === 1 ? activeKeys[0] : null);

  const focus =
    segments.find((segment) => segment.key === focusKey) ?? null;

  const isDim = (key: string) =>
    hovered
      ? hovered !== key
      : activeKeys.length > 0 && !activeKeys.includes(key);

  const single = segments.length === 1;

  const handlers = (key: string) => ({
    onMouseEnter: () => setHovered(key),
    onMouseLeave: () => setHovered(null),
    onClick: () => onSelect(key),
  });

  return (
    <svg
      className="ops-donut"
      viewBox={`0 0 ${SIZE} ${SIZE}`}
      role="img"
      aria-label={ariaLabel}
    >
      <defs>
        <filter
          id={glowId}
          filterUnits="userSpaceOnUse"
          x="0"
          y="0"
          width={SIZE}
          height={SIZE}
        >
          <feGaussianBlur stdDeviation="18" />
        </filter>

        <mask
          id={maskId}
          maskUnits="userSpaceOnUse"
          x="0"
          y="0"
          width={SIZE}
          height={SIZE}
        >
          <circle
            className="ops-donut__reveal"
            cx={CENTER}
            cy={CENTER}
            r={SIZE / 4}
            fill="none"
            stroke="#fff"
            strokeWidth={SIZE / 2}
            pathLength={100}
            transform={`rotate(-90 ${CENTER} ${CENTER})`}
          />

          <rect
            className="ops-donut__reveal-done"
            x="0"
            y="0"
            width={SIZE}
            height={SIZE}
            fill="#fff"
          />
        </mask>
      </defs>

      <circle
        className="ops-donut__track"
        cx={CENTER}
        cy={CENTER}
        r={RADIUS}
        fill="none"
        strokeWidth={THICKNESS}
      />

      {segments.length > 0 && (
        <g mask={`url(#${maskId})`}>
          <g
            className="ops-donut__glow"
            filter={`url(#${glowId})`}
          >
            {segments.map((segment) =>
              single ? (
                <circle
                  key={segment.key}
                  cx={CENTER}
                  cy={CENTER}
                  r={RADIUS}
                  fill="none"
                  stroke={segment.color}
                  strokeWidth={THICKNESS}
                />
              ) : (
                <path
                  key={segment.key}
                  d={arcPath(segment.start, segment.end)}
                  fill="none"
                  stroke={segment.color}
                  strokeWidth={THICKNESS}
                />
              ),
            )}
          </g>

          {segments.map((segment) =>
            single ? (
              <circle
                key={segment.key}
                className={`ops-donut__arc${
                  isDim(segment.key) ? ' is-dim' : ''
                }`}
                cx={CENTER}
                cy={CENTER}
                r={RADIUS}
                fill="none"
                stroke={segment.color}
                strokeWidth={THICKNESS}
                {...handlers(segment.key)}
              />
            ) : (
              <path
                key={segment.key}
                className={`ops-donut__arc${
                  isDim(segment.key) ? ' is-dim' : ''
                }`}
                d={arcPath(segment.start, segment.end)}
                fill="none"
                stroke={segment.color}
                strokeWidth={THICKNESS}
                {...handlers(segment.key)}
              />
            ),
          )}

          {segments.map((segment) => {
            const [x, y] = point(
              single ? START : segment.end,
              RADIUS,
            );

            return (
              <g
                key={segment.key}
                className={`ops-donut__cap${
                  isDim(segment.key) ? ' is-dim' : ''
                }`}
                {...handlers(segment.key)}
              >
                <circle
                  cx={x}
                  cy={y}
                  r={THICKNESS / 2}
                  fill={segment.color}
                />

                <CategoryIcon
                  name={segment.icon}
                  size={22}
                  color="#fff"
                  strokeWidth={2.2}
                  x={x - 11}
                  y={y - 11}
                />
              </g>
            );
          })}
        </g>
      )}

      <g className="ops-donut__labels">
        {segments.map((segment) => {
          const [x, y] = point(segment.mid, LABEL_RADIUS);

          return (
            <text
              key={segment.key}
              x={x}
              y={y}
              textAnchor="middle"
              dominantBaseline="central"
              className={
                isDim(segment.key) ? 'is-dim' : undefined
              }
            >
              {formatPercent(segment.percent, segment.share)}
            </text>
          );
        })}
      </g>

      {focus ? (
        <g className="ops-donut__center">
          <text
            x={CENTER}
            y={CENTER - 13}
            textAnchor="middle"
            className="ops-donut__center-label"
          >
            {focus.label}
          </text>

          <text
            x={CENTER}
            y={CENTER + 16}
            textAnchor="middle"
            className="ops-donut__center-value"
          >
            {formatRounded(focus.value, currency)}
          </text>
        </g>
      ) : (
        segments.length === 0 && (
          <text
            x={CENTER}
            y={CENTER}
            textAnchor="middle"
            dominantBaseline="central"
            className="ops-donut__empty"
          >
            {emptyText}
          </text>
        )
      )}
    </svg>
  );
}

export default DonutChart;