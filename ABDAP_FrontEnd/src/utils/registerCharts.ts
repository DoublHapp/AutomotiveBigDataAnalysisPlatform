import { use } from 'echarts/core'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
// 组件模块
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
export function registerEchartsModules() {
  use([
    PieChart,
    BarChart,
    LineChart,
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent,
    CanvasRenderer,
  ])
}
