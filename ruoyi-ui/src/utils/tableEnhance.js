import tableDefaultProps from 'element-plus/es/components/table/src/table/defaults.mjs'
import tableColumnDefaultProps from 'element-plus/es/components/table/src/table-column/defaults.mjs'

export function installTableEnhanceDefaults() {
  tableDefaultProps.border = {
    type: Boolean,
    default: true
  }

  tableDefaultProps.showOverflowTooltip = {
    type: [Boolean, Object],
    default: () => ({
      placement: 'top',
      showAfter: 250,
      hideAfter: 0,
      enterable: true,
      popperClass: 'xinke-table-overflow-tooltip'
    })
  }

  tableColumnDefaultProps.resizable = {
    type: Boolean,
    default: true
  }
}
