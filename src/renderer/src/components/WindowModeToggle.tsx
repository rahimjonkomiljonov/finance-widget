import type { WindowMode } from '../../../shared/types'

interface Props {
  mode: WindowMode
  onChange: (mode: WindowMode) => void
}

const iconProps = {
  width: 14,
  height: 14,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true
}

// Arrows pointing out to the corners.
function ExpandIcon(): JSX.Element {
  return (
    <svg {...iconProps}>
      <path d="M10 2h4v4M14 2 9 7M6 14H2v-4M2 14l5-5" />
    </svg>
  )
}

// Arrows pointing in toward the center.
function ShrinkIcon(): JSX.Element {
  return (
    <svg {...iconProps}>
      <path d="M13 7H9V3M9 7l5-5M3 9h4v4M7 9l-5 5" />
    </svg>
  )
}

function WindowModeToggle({ mode, onChange }: Props): JSX.Element {
  if (mode === 'widget') {
    return (
      <button
        className="mode-toggle"
        onClick={() => onChange('full')}
        aria-label="Full window"
        title="Expand to full window"
      >
        <ExpandIcon />
      </button>
    )
  }

  return (
    <button
      className="mode-toggle"
      onClick={() => onChange('widget')}
      aria-label="Widget mode"
      title="Shrink to widget"
    >
      <ShrinkIcon />
      <span>Widget</span>
    </button>
  )
}

export default WindowModeToggle
