import { useMemo, useState } from 'react';

import { useOperationsData } from '../../hooks/useOperationsData';
import { pickAnalyticsCurrency } from '../../operations/currency';
import { formatRounded } from '../../operations/format';
import { periodLabel, todayIso } from '../../operations/period';
import type {
  OperationFilters,
  OperationsAccount,
  OperationsSourceTransaction,
} from '../../operations/types';
import { UiIcon } from './Icon';
import '../../styles/operations.css';

interface SpendingWidgetProps {
  accounts: OperationsAccount[];
  transactions?: OperationsSourceTransaction[];
  onOpen?: () => void;
}

const RING = 15.915;

/**
 * Карточка «Траты за месяц» для главной страницы: кольцо по категориям и топ-3.
 */
function SpendingWidget({
  accounts,
  transactions,
  onOpen,
}: SpendingWidgetProps) {
  const [today] = useState(todayIso);

  const filters = useMemo<OperationFilters>(
    () => ({
      query: '',
      categories: [],
      direction: 'ALL',
      accountIds: [],
      excludeTransfers: false,
      period: 'MONTH',
      anchor: today,
    }),
    [today],
  );

  const currency = pickAnalyticsCurrency(accounts);

  const { analytics, loading } = useOperationsData({
    filters,
    accounts,
    transactions,
    currency,
    reloadToken: 0,
  });

  const expenses = analytics?.expenses;
  const categories = expenses?.categories ?? [];

  return (
    <section
      className={`ops-theme ops-widget${loading ? ' is-loading' : ''}`}
      aria-label="Траты за месяц"
    >
      <div className="ops-widget__ring" aria-hidden="true">
        <svg viewBox="0 0 42 42">
          <circle
            cx="21"
            cy="21"
            r={RING}
            className="ops-widget__track"
          />

          {categories.map((category, index) => {
            const length = Math.max(
              category.share * 100 - 1.2,
              0.6,
            );

            const offset = categories
              .slice(0, index)
              .reduce(
                (sum, previous) => sum + previous.share * 100,
                0,
              );

            return (
              <circle
                key={category.category}
                cx="21"
                cy="21"
                r={RING}
                stroke={category.color}
                strokeDasharray={`${length} ${100 - length}`}
                strokeDashoffset={-offset}
              />
            );
          })}
        </svg>
      </div>

      <div className="ops-widget__body">
        <p className="ops-widget__caption">
          Траты, {periodLabel('MONTH', today, today).toLowerCase()}
        </p>

        <strong className="ops-widget__total">
          {formatRounded(expenses?.total ?? 0, currency)}
        </strong>

        <ul className="ops-widget__top">
          {categories.slice(0, 3).map((category) => (
            <li key={category.category}>
              <span
                className="ops-widget__dot"
                style={{ background: category.color }}
              />
              <span>{category.label}</span>
              <span>
                {formatRounded(category.amount, currency)}
              </span>
            </li>
          ))}

          {!loading && categories.length === 0 && (
            <li className="ops-widget__empty">
              В этом месяце трат ещё не было
            </li>
          )}
        </ul>
      </div>

      {onOpen && (
        <button
          type="button"
          className="ops-widget__open"
          onClick={onOpen}
        >
          Все операции
          <UiIcon name="arrowRight" size={16} />
        </button>
      )}
    </section>
  );
}

export default SpendingWidget;