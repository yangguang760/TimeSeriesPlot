1、保证系统java可正常使用(cmd内敲java有返回)
2、生成需要画图的数据（文件名随意，放在plottool文件夹内）
  文件分为四列，逗号分隔；第一列是时间戳，第二列为序列名称，第三列为画图位置，第四列为标记文字（可为空，注意不要有中文和逗号）
  mdsTag.log为示例文件，可参照
3、编辑plot.conf文件，其中
  a、fileName为数据文件名称
  b、indicators为需要画图的序列
  c、按照indicators里的项，分别建立配置项，其中
    i、YType为所在坐标轴，可填 1/2/3/4
    ii、chartType为画图类型，可填line/mark，line为折线图，mark为标记
4、运行genPlot.bat, 在report目录下将生成以运行时间戳命名的文件
5、plot出的图形可缩放/移动,右上角有功能图标
