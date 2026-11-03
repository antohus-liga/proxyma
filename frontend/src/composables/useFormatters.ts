import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const RELATIVE_UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 60 * 60 * 1000],
  ['month', 30 * 24 * 60 * 60 * 1000],
  ['week', 7 * 24 * 60 * 60 * 1000],
  ['day', 24 * 60 * 60 * 1000],
  ['hour', 60 * 60 * 1000],
  ['minute', 60 * 1000],
  ['second', 1000],
]

export function useFormatters() {
  const { locale } = useI18n()
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'

  const dateTimeFormatter = computed(
    () =>
      new Intl.DateTimeFormat(locale.value, {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
        timeZoneName: 'short',
      }),
  )

  const dateFormatter = computed(
    () =>
      new Intl.DateTimeFormat(locale.value, {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      }),
  )

  const relativeFormatter = computed(
    () => new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' }),
  )

  const numberFormatter = computed(() => new Intl.NumberFormat(locale.value))

  const timeZoneLabel = computed(() => {
    const shortName = new Intl.DateTimeFormat(locale.value, {
      timeZone,
      timeZoneName: 'short',
    })
      .formatToParts(new Date())
      .find((part) => part.type === 'timeZoneName')?.value

    return shortName ? `${timeZone} (${shortName})` : timeZone
  })

  function formatDateTime(value: string | Date): string {
    return dateTimeFormatter.value.format(new Date(value))
  }

  function formatDate(value: string | Date): string {
    return dateFormatter.value.format(new Date(value))
  }

  function formatRelative(value: string | Date): string {
    const difference = new Date(value).getTime() - Date.now()
    const absolute = Math.abs(difference)

    for (const [unit, milliseconds] of RELATIVE_UNITS) {
      if (absolute >= milliseconds) {
        return relativeFormatter.value.format(Math.round(difference / milliseconds), unit)
      }
    }

    return relativeFormatter.value.format(Math.round(difference / 1000), 'second')
  }

  function formatRelativeOrAbsolute(value: string | Date): string {
    const absolute = Math.abs(new Date(value).getTime() - Date.now())
    return absolute < 24 * 60 * 60 * 1000 ? formatRelative(value) : formatDateTime(value)
  }

  function formatNumber(value: number): string {
    return numberFormatter.value.format(value)
  }

  return {
    timeZone,
    timeZoneLabel,
    formatDateTime,
    formatDate,
    formatRelative,
    formatRelativeOrAbsolute,
    formatNumber,
  }
}
