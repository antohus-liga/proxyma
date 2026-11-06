<script setup lang="ts">
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { X } from '@lucide/vue'
import PxIconButton from './PxIconButton.vue'

type PxDialogSize = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    size?: PxDialogSize
    closeLabel: string
    busy?: boolean
  }>(),
  {
    description: undefined,
    size: 'sm',
    busy: false,
  },
)

const open = defineModel<boolean>('open', { default: false })

function onOpenChange(value: boolean) {
  if (!value && props.busy) {
    return
  }

  open.value = value
}

function onEscapeKeyDown(event: KeyboardEvent) {
  if (props.busy) {
    event.preventDefault()
  }
}
</script>

<template>
  <DialogRoot :open="open" @update:open="onOpenChange">
    <DialogPortal>
      <DialogOverlay class="px-dialog__overlay" />
      <DialogContent
        class="px-dialog"
        :class="`px-dialog--${size}`"
        @escape-key-down="onEscapeKeyDown"
      >
        <div class="px-dialog__header">
          <DialogTitle class="px-dialog__title">{{ title }}</DialogTitle>
          <DialogDescription v-if="description" class="px-dialog__description">
            {{ description }}
          </DialogDescription>
        </div>

        <PxIconButton
          class="px-dialog__close"
          :label="closeLabel"
          size="sm"
          :disabled="busy"
          @click="onOpenChange(false)"
        >
          <X :size="16" aria-hidden="true" />
        </PxIconButton>

        <div class="px-dialog__body">
          <slot />
        </div>

        <div v-if="$slots.footer" class="px-dialog__footer">
          <slot name="footer" />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<style scoped>
.px-dialog__overlay {
  position: fixed;
  inset: 0;
  z-index: var(--px-z-overlay);
  background: var(--px-color-overlay);
}

.px-dialog {
  position: fixed;
  inset-block-start: 50%;
  inset-inline-start: 50%;
  z-index: var(--px-z-dialog);
  display: flex;
  max-height: min(90dvh, 900px);
  width: min(calc(100vw - 32px), 480px);
  flex-direction: column;
  overflow: auto;
  padding: var(--px-space-6);
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-dialog);
  background: var(--px-color-surface);
  box-shadow: var(--px-shadow-lg);
  transform: translate(-50%, -50%);
}

.px-dialog--md {
  width: min(calc(100vw - 32px), 560px);
}

.px-dialog--lg {
  width: min(calc(100vw - 32px), 720px);
}

.px-dialog__header {
  padding-inline-end: var(--px-space-8);
}

.px-dialog__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h3);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-title);
}

.px-dialog__description {
  margin-block-start: var(--px-space-2);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-control);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.px-dialog__close {
  position: absolute;
  inset-block-start: var(--px-space-3);
  inset-inline-end: var(--px-space-3);
}

.px-dialog__body {
  margin-block-start: var(--px-space-5);
}

.px-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--px-space-2);
  margin-block-start: var(--px-space-6);
  padding-block-start: var(--px-space-4);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

@media (max-width: 479px) {
  .px-dialog {
    inset: 0;
    width: 100%;
    max-height: 100dvh;
    height: 100dvh;
    border: 0;
    border-radius: 0;
    transform: none;
  }

  .px-dialog__footer {
    flex-direction: column-reverse;
    margin-block-start: auto;
    padding-block-start: var(--px-space-4);
  }
}
</style>
