import type { Account, AssistantMessage, BankCard, BankingState, CashbackCategory, CurrencyRate, NavItem, NewsItem, SavingGoal, Transaction, UserProfile } from '../types/banking';

export const navItems: NavItem[] = [
  { key: 'dashboard', label: 'Главная', icon: '⌂' },
  { key: 'accounts', label: 'Счета', icon: '₽' },
  { key: 'payments', label: 'Платежи', icon: '↗' },
  { key: 'currency', label: 'Валюта', icon: '$' },
];

type PersonalSeed = {
  profile: UserProfile;
  accounts: Account[];
  cards: BankCard[];
  transactions: Transaction[];
  assistantMessages: AssistantMessage[];
  goals: SavingGoal[];
  cashbackCategories: CashbackCategory[];
};

export const currencyRates: CurrencyRate[] = [
  { code: 'USD', name: 'Доллар США', buy: 89.5, sell: 90.0, change: 0.42 },
  { code: 'EUR', name: 'Евро', buy: 97.8, sell: 98.5, change: -0.18 },
];

export const news: NewsItem[] = [
  { id: 'news-1', title: 'Как накопительный счёт помогает держать деньги в движении', description: 'Разбираем, почему ежедневный остаток и понятная ставка удобнее обычной копилки.', tag: 'Сбережения', publishedAt: '2026-05-10T09:00:00Z', readMinutes: 4 },
  { id: 'news-2', title: 'Курсы валют: что важно смотреть перед обменом', description: 'Покупка, продажа, комиссия и лимиты — четыре вещи, которые влияют на итоговую сумму.', tag: 'Валюта', publishedAt: '2026-05-09T11:30:00Z', readMinutes: 3 },
  { id: 'news-3', title: 'Безопасные переводы: чек-лист перед отправкой денег', description: 'Проверяйте номер счёта, сумму, назначение и статус операции перед подтверждением.', tag: 'Безопасность', publishedAt: '2026-05-08T15:00:00Z', readMinutes: 5 },
  { id: 'news-4', title: 'Кэшбэк без ловушек: как выбирать категории', description: 'Лучше выбирать категории, где у вас уже есть регулярные траты, а не гнаться за процентом.', tag: 'Кэшбэк', publishedAt: '2026-05-07T14:00:00Z', readMinutes: 4 },
  { id: 'news-5', title: 'Почему важно разделять основные и накопительные счета', description: 'Отдельные счета помогают не тратить деньги, которые отложены на цель.', tag: 'Счета', publishedAt: '2026-05-06T11:10:00Z', readMinutes: 6 },
];

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function keyFromEmail(email?: string | null) {
  return (email ?? 'igor@example.com').trim().toLowerCase();
}

const personalSeeds: Record<string, PersonalSeed> = {
  'igor@example.com': {
    profile: { fullName: 'Игорь', phone: '+7 900 111-22-33', email: 'igor@example.com', role: 'user', theme: 'light', cashbackLevel: 'Premium 5%', city: 'Москва' },
    accounts: [
      { id: 'igor-main-rub', name: 'Основной счёт Игоря', number: '40817810000000012345', balance: 150000, currency: 'RUB', type: 'debit', status: 'active', openedAt: '2025-09-12T10:00:00Z' },
      { id: 'igor-saving-rub', name: 'Накопления на квартиру', number: '40817810000000056789', balance: 315000, currency: 'RUB', type: 'saving', status: 'active', interestRate: 12.5, openedAt: '2025-10-01T12:00:00Z' },
      { id: 'igor-usd', name: 'USD счёт Игоря', number: '40817840000000099887', balance: 1000, currency: 'USD', type: 'debit', status: 'active', openedAt: '2025-12-18T09:30:00Z' },
      { id: 'igor-eur', name: 'EUR счёт Игоря', number: '40817978000000077881', balance: 1000, currency: 'EUR', type: 'debit', status: 'active', openedAt: '2026-02-02T09:30:00Z' },
    ],
    cards: [
      { id: 'igor-card-black', name: 'МИК Black', maskedNumber: '**** 1234', paymentSystem: 'МИР', balance: 82340.12, currency: 'RUB', expiresAt: '08/29', color: 'black', status: 'active', cashback: 5, dailyLimit: 150000 },
      { id: 'igor-card-drive', name: 'МИК Drive', maskedNumber: '**** 5678', paymentSystem: 'МИР', balance: 45600, currency: 'RUB', expiresAt: '02/30', color: 'yellow', status: 'active', cashback: 7, dailyLimit: 90000 },
      { id: 'igor-card-travel', name: 'МИК Travel', maskedNumber: '**** 0442', paymentSystem: 'Visa', balance: 780, currency: 'EUR', expiresAt: '12/30', color: 'violet', status: 'active', cashback: 4, dailyLimit: 180000 },
    ],
    transactions: [
      { id: 'igor-tx-1', title: 'Зарплата', category: 'Доход', amount: 95000, currency: 'RUB', createdAt: '2026-05-19T13:00:00Z', status: 'success', accountId: 'igor-main-rub', type: 'income' },
      { id: 'igor-tx-2', title: 'Супермаркет', category: 'Покупки', amount: -3240, currency: 'RUB', createdAt: '2026-05-18T12:40:00Z', status: 'success', accountId: 'igor-main-rub', type: 'outcome' },
      { id: 'igor-tx-3', title: 'Такси', category: 'Транспорт', amount: -760, currency: 'RUB', createdAt: '2026-05-17T21:30:00Z', status: 'success', accountId: 'igor-main-rub', type: 'outcome' },
      { id: 'igor-tx-4', title: 'Кофейня', category: 'Кафе', amount: -420, currency: 'RUB', createdAt: '2026-05-17T09:20:00Z', status: 'success', accountId: 'igor-main-rub', type: 'outcome' },
      { id: 'igor-tx-5', title: 'Начисление процентов', category: 'Накопления', amount: 2760, currency: 'RUB', createdAt: '2026-05-16T09:00:00Z', status: 'success', accountId: 'igor-saving-rub', type: 'income' },
    ],
    assistantMessages: [{ id: 'igor-assistant-1', role: 'assistant', text: 'Игорь, я помогу проверить счета, расходы, цели и операции.', createdAt: '2026-05-10T09:00:00Z' }],
    goals: [
      { id: 'igor-goal-1', title: 'Финансовая подушка', target: 500000, saved: 315000, currency: 'RUB', icon: '◌' },
      { id: 'igor-goal-2', title: 'Путешествие', target: 180000, saved: 68500, currency: 'RUB', icon: '✈' },
      { id: 'igor-goal-3', title: 'Новый ноутбук', target: 140000, saved: 93000, currency: 'RUB', icon: '⌘' },
    ],
    cashbackCategories: [
      { id: 'igor-cashback-1', title: 'Супермаркеты', percent: 5, icon: '▣', selected: true },
      { id: 'igor-cashback-2', title: 'Транспорт', percent: 7, icon: '↗', selected: true },
      { id: 'igor-cashback-3', title: 'Кафе', percent: 10, icon: '☕', selected: false },
      { id: 'igor-cashback-4', title: 'Подписки', percent: 8, icon: '▣', selected: true },
    ],
  },
  'maxim@example.com': {
    profile: { fullName: 'Максим', phone: '+7 900 444-55-66', email: 'maxim@example.com', role: 'user', theme: 'light', cashbackLevel: 'Student 3%', city: 'Обнинск' },
    accounts: [
      { id: 'maxim-main-rub', name: 'Основной счёт Максима', number: '40817810000000023456', balance: 150000, currency: 'RUB', type: 'debit', status: 'active', openedAt: '2025-08-20T10:00:00Z' },
      { id: 'maxim-saving-rub', name: 'Учёба и техника', number: '40817810000000067890', balance: 220000, currency: 'RUB', type: 'saving', status: 'active', interestRate: 12.5, openedAt: '2025-11-05T12:00:00Z' },
      { id: 'maxim-usd', name: 'USD счёт Максима', number: '40817840000000088771', balance: 1000, currency: 'USD', type: 'debit', status: 'active', openedAt: '2026-01-10T09:30:00Z' },
      { id: 'maxim-eur', name: 'EUR счёт Максима', number: '40817978000000066331', balance: 1000, currency: 'EUR', type: 'debit', status: 'active', openedAt: '2026-02-14T09:30:00Z' },
    ],
    cards: [
      { id: 'maxim-card-main', name: 'МИК Black', maskedNumber: '**** 2456', paymentSystem: 'МИР', balance: 62000, currency: 'RUB', expiresAt: '03/30', color: 'black', status: 'active', cashback: 3, dailyLimit: 120000 },
      { id: 'maxim-card-student', name: 'МИК Student', maskedNumber: '**** 8841', paymentSystem: 'Mastercard', balance: 31000, currency: 'RUB', expiresAt: '09/29', color: 'platinum', status: 'active', cashback: 6, dailyLimit: 80000 },
    ],
    transactions: [
      { id: 'maxim-tx-1', title: 'Стипендия', category: 'Доход', amount: 18000, currency: 'RUB', createdAt: '2026-05-19T11:30:00Z', status: 'success', accountId: 'maxim-main-rub', type: 'income' },
      { id: 'maxim-tx-2', title: 'Общежитие в Обнинске', category: 'Жильё', amount: -6500, currency: 'RUB', createdAt: '2026-05-18T08:00:00Z', status: 'success', accountId: 'maxim-main-rub', type: 'outcome' },
      { id: 'maxim-tx-3', title: 'Проездной', category: 'Транспорт', amount: -2400, currency: 'RUB', createdAt: '2026-05-17T19:15:00Z', status: 'success', accountId: 'maxim-main-rub', type: 'outcome' },
      { id: 'maxim-tx-4', title: 'Учебная подписка', category: 'Образование', amount: -299, currency: 'RUB', createdAt: '2026-05-16T10:10:00Z', status: 'success', accountId: 'maxim-main-rub', type: 'outcome' },
      { id: 'maxim-tx-5', title: 'Пополнение накоплений', category: 'Накопления', amount: -15000, currency: 'RUB', createdAt: '2026-05-15T14:00:00Z', status: 'success', accountId: 'maxim-main-rub', type: 'transfer' },
    ],
    assistantMessages: [{ id: 'maxim-assistant-1', role: 'assistant', text: 'Максим, я покажу расходы по категориям, баланс и цели.', createdAt: '2026-05-10T09:00:00Z' }],
    goals: [
      { id: 'maxim-goal-1', title: 'Ноутбук для учёбы', target: 180000, saved: 92000, currency: 'RUB', icon: '⌘' },
      { id: 'maxim-goal-2', title: 'Курсы по разработке', target: 90000, saved: 38000, currency: 'RUB', icon: '◌' },
      { id: 'maxim-goal-3', title: 'Учебная практика', target: 60000, saved: 24000, currency: 'RUB', icon: '✈' },
    ],
    cashbackCategories: [
      { id: 'maxim-cashback-1', title: 'Транспорт', percent: 7, icon: '↗', selected: true },
      { id: 'maxim-cashback-2', title: 'Кино и развлечения', percent: 6, icon: '◆', selected: true },
      { id: 'maxim-cashback-3', title: 'Подписки', percent: 8, icon: '▣', selected: true },
      { id: 'maxim-cashback-4', title: 'Супермаркеты', percent: 3, icon: '▣', selected: false },
    ],
  },
  'nikolay@example.com': {
    profile: { fullName: 'Николай', phone: '+7 900 777-88-99', email: 'nikolay@example.com', role: 'user', theme: 'light', cashbackLevel: 'Student 2%', city: 'Грозный' },
    accounts: [
      { id: 'nikolay-main-rub', name: 'Основной счёт Николая', number: '40817810000000034567', balance: 150000, currency: 'RUB', type: 'debit', status: 'active', openedAt: '2025-07-18T10:00:00Z' },
      { id: 'nikolay-saving-rub', name: 'Учёба и техника', number: '40817810000000078901', balance: 185000, currency: 'RUB', type: 'saving', status: 'active', interestRate: 12.5, openedAt: '2025-12-01T12:00:00Z' },
      { id: 'nikolay-usd', name: 'USD счёт Николая', number: '40817840000000077662', balance: 1000, currency: 'USD', type: 'debit', status: 'active', openedAt: '2026-01-22T09:30:00Z' },
      { id: 'nikolay-eur', name: 'EUR счёт Николая', number: '40817978000000055228', balance: 1000, currency: 'EUR', type: 'debit', status: 'active', openedAt: '2026-03-03T09:30:00Z' },
    ],
    cards: [
      { id: 'nikolay-card-main', name: 'МИК Black', maskedNumber: '**** 3498', paymentSystem: 'МИР', balance: 58000, currency: 'RUB', expiresAt: '06/29', color: 'black', status: 'active', cashback: 2, dailyLimit: 100000 },
      { id: 'nikolay-card-student', name: 'МИК Student', maskedNumber: '**** 7120', paymentSystem: 'Visa', balance: 27500, currency: 'RUB', expiresAt: '12/30', color: 'yellow', status: 'active', cashback: 5, dailyLimit: 70000 },
    ],
    transactions: [
      { id: 'nikolay-tx-1', title: 'Стипендия', category: 'Доход', amount: 22000, currency: 'RUB', createdAt: '2026-05-19T10:00:00Z', status: 'success', accountId: 'nikolay-main-rub', type: 'income' },
      { id: 'nikolay-tx-2', title: 'Учебные материалы', category: 'Образование', amount: -2800, currency: 'RUB', createdAt: '2026-05-18T16:20:00Z', status: 'success', accountId: 'nikolay-main-rub', type: 'outcome' },
      { id: 'nikolay-tx-3', title: 'Общежитие', category: 'Жильё', amount: -6200, currency: 'RUB', createdAt: '2026-05-17T12:00:00Z', status: 'success', accountId: 'nikolay-main-rub', type: 'outcome' },
      { id: 'nikolay-tx-4', title: 'Курсы английского', category: 'Образование', amount: -12400, currency: 'RUB', createdAt: '2026-05-16T15:45:00Z', status: 'success', accountId: 'nikolay-main-rub', type: 'outcome' },
      { id: 'nikolay-tx-5', title: 'Пополнение цели', category: 'Накопления', amount: -10000, currency: 'RUB', createdAt: '2026-05-15T11:00:00Z', status: 'success', accountId: 'nikolay-main-rub', type: 'transfer' },
    ],
    assistantMessages: [{ id: 'nikolay-assistant-1', role: 'assistant', text: 'Николай, здесь можно посмотреть личные операции, расходы и цели.', createdAt: '2026-05-10T09:00:00Z' }],
    goals: [
      { id: 'nikolay-goal-1', title: 'Ноутбук для учёбы', target: 160000, saved: 85000, currency: 'RUB', icon: '⌘' },
      { id: 'nikolay-goal-2', title: 'Учебная поездка', target: 120000, saved: 46000, currency: 'RUB', icon: '✈' },
      { id: 'nikolay-goal-3', title: 'Книги и курсы', target: 80000, saved: 27500, currency: 'RUB', icon: '◌' },
    ],
    cashbackCategories: [
      { id: 'nikolay-cashback-1', title: 'Кафе', percent: 6, icon: '☕', selected: true },
      { id: 'nikolay-cashback-2', title: 'Образование', percent: 5, icon: '⌘', selected: true },
      { id: 'nikolay-cashback-3', title: 'Транспорт', percent: 3, icon: '↗', selected: true },
      { id: 'nikolay-cashback-4', title: 'Супермаркеты', percent: 4, icon: '▣', selected: false },
    ],
  },
};

function nameFromEmail(email?: string | null) {
  const prefix = email?.split('@')[0]?.trim();
  if (!prefix) return 'Новый пользователь';

  return prefix
    .split(/[._-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ') || 'Новый пользователь';
}

function createEmptyBankingState(email?: string | null): BankingState {
  return {
    profile: {
      fullName: nameFromEmail(email),
      phone: '',
      email: email ?? '',
      role: 'user',
      theme: 'light',
      cashbackLevel: 'Базовый',
      city: '',
    },
    accounts: [],
    cards: [],
    transactions: [],
    rates: clone(currencyRates),
    news: [],
    assistantMessages: [],
    goals: [],
    cashbackCategories: [],
  };
}

export function createBankingStateForEmail(email?: string | null): BankingState {
  const seed = personalSeeds[keyFromEmail(email)];

  if (!seed) {
    return createEmptyBankingState(email);
  }

  return {
    profile: clone(seed.profile),
    accounts: clone(seed.accounts),
    cards: clone(seed.cards),
    transactions: clone(seed.transactions),
    rates: clone(currencyRates),
    news: clone(news),
    assistantMessages: clone(seed.assistantMessages),
    goals: clone(seed.goals),
    cashbackCategories: clone(seed.cashbackCategories),
  };
}

export const profile = createBankingStateForEmail('igor@example.com').profile;
export const accounts = createBankingStateForEmail('igor@example.com').accounts;
export const cards = createBankingStateForEmail('igor@example.com').cards;
export const transactions = createBankingStateForEmail('igor@example.com').transactions;
export const assistantMessages = createBankingStateForEmail('igor@example.com').assistantMessages;
export const goals = createBankingStateForEmail('igor@example.com').goals;
export const cashbackCategories = createBankingStateForEmail('igor@example.com').cashbackCategories;

export const quickPrompts = ['Какой общий баланс?', 'Покажи последние расходы', 'Какие у меня цели?', 'Какие курсы валют?', 'Как выполнить перевод?'];

export const initialBankingState: BankingState = createBankingStateForEmail('igor@example.com');
