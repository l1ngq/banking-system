import type { Toast } from '../hooks/useBankingState';

interface ToastStackProps { toasts: Toast[]; onRemove: (id: string) => void; }

function ToastStack({ toasts, onRemove }: ToastStackProps) {
  if (!toasts.length) return null;
  return (
    <div className="toast-stack">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast--${toast.type}`}>
          <div><strong>{toast.title}</strong>{toast.text && <p>{toast.text}</p>}</div>
          <button type="button" onClick={() => onRemove(toast.id)}>×</button>
        </div>
      ))}
    </div>
  );
}

export default ToastStack;
