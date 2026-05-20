import type { Page } from '../types/banking';

function Footer({ onNavigate }: { onNavigate: (page: Page) => void }) {
  return (
    <footer className="footer">
      <div className="footer__top">
        <div><strong>МИК Банк</strong><p>Учебный банковский личный кабинет для работы со счетами, операциями и валютой.</p></div>
        <div className="footer__status"><span>Счета</span><span>Переводы</span><span>Валюта</span></div>
      </div>
      <div className="footer__nav">
        <button type="button" onClick={() => onNavigate('accounts')}>Счета</button>
        <button type="button" onClick={() => onNavigate('payments')}>Платежи</button>
        <button type="button" onClick={() => onNavigate('currency')}>Валюта</button>
        <button type="button" onClick={() => onNavigate('assistant')}>Ассистент</button>
      </div>
      <div className="footer__info"><p>Основные банковские действия доступны в личном кабинете, дополнительные разделы отмечены как демо.</p></div>
      <div className="footer__bottom"><span>© 2026 МИК Банк</span><span>Курсовая работа</span></div>
    </footer>
  );
}

export default Footer;
