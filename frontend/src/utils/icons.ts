import {
  Bell,
  Briefcase,
  Calendar,
  ChartColumn,
  ClipboardList,
  Database,
  FileText,
  Folder,
  KeyRound,
  Layers,
  LayoutDashboard,
  ListTree,
  Mail,
  Settings,
  ShieldCheck,
  Upload,
  UserRound,
  Users,
  Circle,
  type LucideIcon,
} from 'lucide-react';

/**
 * Catálogo cerrado de iconos que puede usar una entrada del menú.
 *
 * El backend guarda solo el nombre corto (`users`, `chart`...) y aquí se traduce al
 * dibujo. Es un catálogo cerrado a propósito: si se aceptara cualquier icono de la
 * librería, el selector tendría mil opciones y el menú acabaría sin coherencia visual.
 */
export const MENU_ICONS: Record<string, LucideIcon> = {
  dashboard: LayoutDashboard,
  users: Users,
  shield: ShieldCheck,
  menu: ListTree,
  user: UserRound,
  chart: ChartColumn,
  file: FileText,
  folder: Folder,
  upload: Upload,
  database: Database,
  calendar: Calendar,
  mail: Mail,
  bell: Bell,
  key: KeyRound,
  layers: Layers,
  briefcase: Briefcase,
  clipboard: ClipboardList,
  settings: Settings,
};

/**
 * Devuelve el componente de icono para un nombre del catálogo.
 *
 * @param name nombre guardado en la entrada del menú; puede venir vacío
 * @returns el icono correspondiente, o un círculo neutro si el nombre no existe
 */
export function menuIcon(name?: string | null): LucideIcon {
  return (name && MENU_ICONS[name]) || Circle;
}
