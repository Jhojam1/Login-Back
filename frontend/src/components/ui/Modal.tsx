import { X } from 'lucide-react';
import { useEffect, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

interface ModalProps {
  /** Si el diálogo está visible. */
  isOpen: boolean;
  /** Título del diálogo. */
  title: string;
  /** Texto opcional bajo el título. */
  description?: string;
  /** Contenido del diálogo (normalmente un formulario). */
  children: ReactNode;
  /** Se ejecuta al pulsar Escape, el fondo o el botón de cerrar. */
  onClose: () => void;
}

/**
 * Diálogo centrado para los formularios de crear y editar.
 *
 * Escape cierra. Mientras está abierto se bloquea el desplazamiento del fondo.
 *
 * @param props estado, textos, contenido y acción de cierre
 */
export function Modal({ isOpen, title, description, children, onClose }: ModalProps) {
  useEffect(() => {
    if (!isOpen) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  // El diálogo se dibuja directamente en <body> mediante un portal, fuera del árbol de
  // la página. Es imprescindible: las páginas entran con una animación que les deja un
  // `transform`, y cualquier ancestro con `transform` convierte `position: fixed` en
  // relativo a ese ancestro. Sin el portal, el fondo oscuro y el diálogo quedarían
  // encerrados en el recuadro de la página en vez de cubrir la ventana.
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-4">
      <div
        className="absolute inset-0 animate-fade bg-slate-900/40 backdrop-blur-[2px]"
        onClick={onClose}
        aria-hidden
      />

      {/*
        En móvil el diálogo sale desde abajo y ocupa como mucho el 92% de la altura, como
        una hoja de las apps nativas: deja ver que hay algo detrás y el pulgar llega a los
        botones. Desde `sm` vuelve a ser un diálogo centrado. En ambos casos la altura
        máxima impide que sobresalga de la ventana por mucho contenido que tenga.
      */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative z-10 flex max-h-[92dvh] w-full animate-rise flex-col overflow-hidden rounded-t-2xl bg-white shadow-2xl sm:max-h-full sm:max-w-lg sm:rounded-2xl"
      >
        {/* shrink-0: la cabecera no se comprime cuando el cuerpo es largo */}
        <div className="flex shrink-0 items-start justify-between gap-4 border-b border-slate-100 px-5 py-4 sm:px-6">
          <div>
            <h2 className="text-base font-semibold text-slate-900">{title}</h2>
            {description && <p className="mt-0.5 text-sm text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="-mr-1 rounded-lg p-1 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
          >
            <X className="size-5" />
          </button>
        </div>

        {/*
          min-h-0 es imprescindible y no es evidente: por defecto un elemento flexible
          tiene min-height:auto, que le impide encogerse por debajo de su contenido. Sin
          esto, overflow-y-auto no llega a activarse nunca, el cuerpo empuja al dialogo
          mas alla de su altura maxima y, al estar centrado, se corta por arriba.
        */}
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 py-5 sm:px-6">
          {children}
        </div>
      </div>
    </div>,
    document.body,
  );
}

/**
 * Pie de los formularios dentro de un diálogo.
 *
 * En móvil los botones ocupan todo el ancho y el principal queda arriba, más cerca del
 * pulgar; desde `sm` se alinean a la derecha con su tamaño natural.
 *
 * @param props botones a mostrar
 */
export function ModalFooter({ children }: { children: ReactNode }) {
  return (
    <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end [&>*]:w-full sm:[&>*]:w-auto">
      {children}
    </div>
  );
}
