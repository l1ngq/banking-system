import type { Account, Action, CurrencyRate } from '../types/banking';
import { formatMoney } from '../utils/formatters';

interface CurrencyPageProps { rates: CurrencyRate[]; accounts: Account[]; onAction: (action: Action) => void; }

function CurrencyPage({ rates, accounts, onAction }: CurrencyPageProps) {
  return (
    <div className="page-grid">
      <section className="page-hero"><div><p className="eyebrow">валютные операции</p><h2>Валюта</h2><p>Просматривайте курсы валют и выполняйте обмен между счетами.</p></div><button onClick={() => onAction('exchange')}>Обменять</button></section>
      <div className="content__row">
        <section className="card"><h2>Курсы валют</h2><div className="rates-list">{rates.map((rate) => <div className="rate-row" key={rate.code}><div><strong>{rate.code}</strong><span>{rate.name}</span></div><div><span>Покупка {rate.buy.toFixed(2)}</span><strong>{rate.sell.toFixed(2)} ₽</strong></div></div>)}</div></section>
        <section className="card"><h2>Валютные счета</h2><div className="rates-list">{accounts.filter((account) => account.currency !== 'RUB').map((account) => <div className="rate-row" key={account.id}><div><strong>{account.name}</strong><span>{account.number.slice(-8)}</span></div><strong>{formatMoney(account.balance, account.currency)}</strong></div>)}</div></section>
      </div>
    </div>
  );
}

export default CurrencyPage;
