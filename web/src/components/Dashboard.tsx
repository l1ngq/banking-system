import type { Action, BankingState, Page, Transaction } from '../types/banking';
import { formatDateTime, formatMoney } from '../utils/formatters';
import { getMonthlyIncome, getMonthlyOutcome } from '../utils/calculations';

interface DashboardProps { state: BankingState; summary: { totalBalance: number; activeAccounts: BankingState['accounts']; latestTransactions: Transaction[] }; onAction: (action: Action) => void; onNavigate: (page: Page) => void; }

function Dashboard({ state, summary, onAction, onNavigate }: DashboardProps) {
  const income = getMonthlyIncome(state.transactions);
  const outcome = getMonthlyOutcome(state.transactions);
  return (
    <div className="dashboard">
      <section className="hero-card">
        <div className="hero-card__content"><p className="eyebrow">личный кабинет</p><h2>{formatMoney(summary.totalBalance, 'RUB')}</h2><p>Общий баланс по вашим активным счетам.</p></div>
        <div className="hero-card__visual"><span /><span /><span /></div>
        <div className="hero-card__actions"><button onClick={() => onAction('topup')}>Пополнить</button><button onClick={() => onAction('transfer')}>Перевести</button><button onClick={() => onAction('pay')}>Оплатить</button><button onClick={() => onAction('exchange')}>Обмен</button></div>
      </section>
      <div className="quick-grid">
        <article className="quick-card"><span>Активные счета</span><strong>{summary.activeAccounts.length}</strong><small>Ваши открытые счета</small></article>
        <article className="quick-card quick-card--income"><span>Доходы</span><strong>{formatMoney(income, 'RUB')}</strong><small>За выбранный период</small></article>
        <article className="quick-card quick-card--outcome"><span>Расходы</span><strong>{formatMoney(outcome, 'RUB')}</strong><small>Платежи и переводы</small></article>
      </div>
      <div className="content__row">
        <section className="card"><div className="section-head"><h2>Последние операции</h2><button className="text-button" onClick={() => onNavigate('payments')}>Все</button></div><div className="transactions">{summary.latestTransactions.map((tx) => <div key={tx.id} className="transaction"><div><strong>{tx.title}</strong><span>{formatDateTime(tx.createdAt)} · {tx.category}</span></div><strong className={tx.amount >= 0 ? 'positive' : 'negative'}>{formatMoney(tx.amount, tx.currency)}</strong></div>)}</div></section>
        <section className="card"><div className="section-head"><h2>Курсы</h2><button className="text-button" onClick={() => onNavigate('currency')}>Обменять</button></div><div className="rates-list">{state.rates.map((rate) => <div className="rate-row" key={rate.code}><div><strong>{rate.code}</strong><span>{rate.name}</span></div><strong>{rate.sell.toFixed(2)} ₽</strong></div>)}</div></section>
      </div>
    </div>
  );
}

export default Dashboard;
