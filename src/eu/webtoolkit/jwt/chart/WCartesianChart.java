/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.chart;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.ref.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.*;
import javax.servlet.*;
import eu.webtoolkit.jwt.*;
import eu.webtoolkit.jwt.chart.*;
import eu.webtoolkit.jwt.utils.*;
import eu.webtoolkit.jwt.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cartesian chart.
 * <p>
 * 
 * A cartesian chart is a chart that uses X and Y axes. It can display one or
 * multiple data series, which each may be rendered using bars, lines, areas, or
 * points.
 * <p>
 * To use a cartesian chart, the minimum you need to do is set a model using
 * {@link WAbstractChart#setModel(WAbstractItemModel model)
 * WAbstractChart#setModel()}, set the model column that holds the X data using
 * {@link WCartesianChart#setXSeriesColumn(int modelColumn) setXSeriesColumn()},
 * and add one or more series using
 * {@link WCartesianChart#addSeries(WDataSeries series) addSeries()}. Each
 * series corresponds to one data column that holds Y data.
 * <p>
 * A cartesian chart is either a {@link ChartType#CategoryChart CategoryChart}
 * or a {@link ChartType#ScatterPlot ScatterPlot}.
 * <p>
 * In a <b>CategoryChart</b>, the X series represent different categories, which
 * are listed consecutively in model row order. The X axis scale is set to
 * {@link AxisScale#CategoryScale CategoryScale}.
 * <p>
 * <div align="center"> <img src="doc-files//ChartWCartesianChart-1.png"
 * alt="A category chart with bar series">
 * <p>
 * <strong>A category chart with bar series</strong>
 * </p>
 * </div> Each series may be rendered differently, and this is configured in the
 * data series (see {@link WDataSeries} for more information).
 * <p>
 * In a <b>ScatterPlot</b>, the X series data are interpreted as numbers on a
 * numerical scale. The scale for the X axis defaults to a
 * {@link AxisScale#LinearScale LinearScale}, but this may be changed to a
 * {@link AxisScale#DateScale DateScale} when the X series contains dates (of
 * type {@link eu.webtoolkit.jwt.WDate}) to create a time series chart, or to a
 * {@link AxisScale#LogScale LogScale}. A ScatterPlot supports the same types of
 * data series as a CategoryChart, but does not support stacking. In a scatter
 * plot, the X series do not need to be ordered in increasing values, and may be
 * set differently for each dataseries using
 * {@link WDataSeries#setXSeriesColumn(int modelColumn)
 * WDataSeries#setXSeriesColumn()}.
 * <p>
 * <div align="center"> <img src="doc-files//ChartWCartesianChart-2.png"
 * alt="A time series scatter plot with line series">
 * <p>
 * <strong>A time series scatter plot with line series</strong>
 * </p>
 * </div> Missing data in a model series Y values is interpreted as a
 * <i>break</i>. For curve-like series, this breaks the curve (or line).
 * <p>
 * The cartesian chart has support for dual Y axes. Each data series may be
 * bound to one of the two Y axes. By default, only the first Y axis is
 * displayed. To show the second Y axis you will need to call:
 * <p>
 * By default a chart has a horizontal X axis and a vertical Y axis, which
 * corresponds to a {@link Orientation#Vertical Vertical} orientation. The
 * orientation may be changed to {@link Orientation#Horizontal Horizontal} using
 * {@link WCartesianChart#setOrientation(Orientation orientation)
 * setOrientation()}.
 * <p>
 * The styling of the series data are dictated by a palette which may be set
 * using {@link WAbstractChart#setPalette(WChartPalette palette)
 * WAbstractChart#setPalette()}, but may be overridden by settings in each data
 * series.
 * <p>
 * <h3>CSS</h3>
 * <p>
 * Styling through CSS is not applicable.
 * <p>
 * <h3>Client-side interaction</h3>
 * <p>
 * WCartesianChart has several features that allow interaction with the chart
 * without server roundtrips. These features include zoom, pan, crosshair and
 * follow curve functionality.
 * <p>
 * <p>
 * <i><b>Note: </b>Client side interaction is only available if the chart is
 * drawn on an HTML canvas. This is the default rendering method on modern
 * browsers, see
 * {@link WPaintedWidget#setPreferredMethod(WPaintedWidget.Method method)
 * WPaintedWidget#setPreferredMethod()}
 * <p>
 * Some features are currently not supported in interactive mode:
 * <ul>
 * <li>Axes set at ZeroValue position will not always be drawn correctly. They
 * may be clipped off outside of the chart area, and when zooming, the axis
 * ticks will change size.</li>
 * <li>{@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()}
 * is incompatible with interactive mode</li>
 * </ul>
 * </i>
 * </p>
 * 
 * @see WDataSeries
 * @see WAxis
 * @see WPieChart
 */
public class WCartesianChart extends WAbstractChart {
	private static Logger logger = LoggerFactory
			.getLogger(WCartesianChart.class);

	/**
	 * Creates a new cartesian chart.
	 * <p>
	 * Creates a cartesian chart of type {@link ChartType#CategoryChart
	 * CategoryChart}.
	 */
	public WCartesianChart(WContainerWidget parent) {
		super(parent);
		this.interface_ = new WChart2DImplementation(this);
		this.orientation_ = Orientation.Vertical;
		this.XSeriesColumn_ = -1;
		this.type_ = ChartType.CategoryChart;
		this.series_ = new ArrayList<WDataSeries>();
		this.xAxis_ = new WCartesianChart.AxisStruct();
		this.yAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
		this.barMargin_ = 0;
		this.legend_ = new WLegend();
		this.axisPadding_ = 5;
		this.borderPen_ = new WPen(PenStyle.NoPen);
		this.textPen_ = new WPen();
		this.chartArea_ = null;
		this.hasDeferredToolTips_ = false;
		this.jsDefined_ = false;
		this.zoomEnabled_ = false;
		this.panEnabled_ = false;
		this.rubberBandEnabled_ = true;
		this.crosshairEnabled_ = false;
		this.crosshairColor_ = WColor.black;
		this.crosshairYAxis_ = 0;
		this.seriesSelectionEnabled_ = false;
		this.selectedSeries_ = null;
		this.followCurve_ = null;
		this.curveManipulationEnabled_ = false;
		this.cObjCreated_ = false;
		this.seriesSelected_ = new Signal2<WDataSeries, WPointF>();
		this.jsSeriesSelected_ = new JSignal2<Double, Double>(this,
				"seriesSelected") {
		};
		this.curvePaths_ = new HashMap<WDataSeries, WJavaScriptHandle<WPainterPath>>();
		this.freePainterPaths_ = new ArrayList<WJavaScriptHandle<WPainterPath>>();
		this.curveTransforms_ = new HashMap<WDataSeries, WJavaScriptHandle<WTransform>>();
		this.freeTransforms_ = new ArrayList<WJavaScriptHandle<WTransform>>();
		this.freePens_ = new ArrayList<WJavaScriptHandle<WPen>>();
		this.curveLabels_ = new ArrayList<CurveLabel>();
		this.axisSliderWidgets_ = new ArrayList<WAxisSliderWidget>();
		this.wheelActions_ = new HashMap<EnumSet<KeyboardModifier>, InteractiveAction>();
		this.loadTooltip_ = new JSignal2<Double, Double>(this, "loadTooltip") {
		};
		this.barTooltips_ = new ArrayList<WCartesianChart.BarTooltip>();
		this.init();
	}

	/**
	 * Creates a new cartesian chart.
	 * <p>
	 * Calls {@link #WCartesianChart(WContainerWidget parent)
	 * this((WContainerWidget)null)}
	 */
	public WCartesianChart() {
		this((WContainerWidget) null);
	}

	/**
	 * Creates a new cartesian chart.
	 * <p>
	 * Creates a cartesian chart of the indicated <code>type</code>.
	 */
	public WCartesianChart(ChartType type, WContainerWidget parent) {
		super(parent);
		this.interface_ = new WChart2DImplementation(this);
		this.orientation_ = Orientation.Vertical;
		this.XSeriesColumn_ = -1;
		this.type_ = type;
		this.series_ = new ArrayList<WDataSeries>();
		this.xAxis_ = new WCartesianChart.AxisStruct();
		this.yAxes_ = new ArrayList<WCartesianChart.AxisStruct>();
		this.barMargin_ = 0;
		this.legend_ = new WLegend();
		this.axisPadding_ = 5;
		this.borderPen_ = new WPen(PenStyle.NoPen);
		this.textPen_ = new WPen();
		this.chartArea_ = null;
		this.hasDeferredToolTips_ = false;
		this.jsDefined_ = false;
		this.zoomEnabled_ = false;
		this.panEnabled_ = false;
		this.rubberBandEnabled_ = true;
		this.crosshairEnabled_ = false;
		this.crosshairColor_ = WColor.black;
		this.crosshairYAxis_ = 0;
		this.seriesSelectionEnabled_ = false;
		this.selectedSeries_ = null;
		this.followCurve_ = null;
		this.curveManipulationEnabled_ = false;
		this.cObjCreated_ = false;
		this.seriesSelected_ = new Signal2<WDataSeries, WPointF>();
		this.jsSeriesSelected_ = new JSignal2<Double, Double>(this,
				"seriesSelected") {
		};
		this.curvePaths_ = new HashMap<WDataSeries, WJavaScriptHandle<WPainterPath>>();
		this.freePainterPaths_ = new ArrayList<WJavaScriptHandle<WPainterPath>>();
		this.curveTransforms_ = new HashMap<WDataSeries, WJavaScriptHandle<WTransform>>();
		this.freeTransforms_ = new ArrayList<WJavaScriptHandle<WTransform>>();
		this.freePens_ = new ArrayList<WJavaScriptHandle<WPen>>();
		this.curveLabels_ = new ArrayList<CurveLabel>();
		this.axisSliderWidgets_ = new ArrayList<WAxisSliderWidget>();
		this.wheelActions_ = new HashMap<EnumSet<KeyboardModifier>, InteractiveAction>();
		this.loadTooltip_ = new JSignal2<Double, Double>(this, "loadTooltip") {
		};
		this.barTooltips_ = new ArrayList<WCartesianChart.BarTooltip>();
		this.init();
	}

	/**
	 * Creates a new cartesian chart.
	 * <p>
	 * Calls {@link #WCartesianChart(ChartType type, WContainerWidget parent)
	 * this(type, (WContainerWidget)null)}
	 */
	public WCartesianChart(ChartType type) {
		this(type, (WContainerWidget) null);
	}

	public void remove() {
		while (!this.yAxes_.isEmpty()) {
			;
			this.yAxes_.remove(this.yAxes_.size() - 1);
		}
		;
		List<WAxisSliderWidget> copy = new ArrayList<WAxisSliderWidget>(
				this.axisSliderWidgets_);
		this.axisSliderWidgets_.clear();
		for (int i = 0; i < copy.size(); ++i) {
			copy.get(i).setSeries((WDataSeries) null);
		}
		for (int i = 0; i < this.series_.size(); ++i) {
			;
		}
		super.remove();
	}

	/**
	 * Sets the chart type.
	 * <p>
	 * The chart type determines how (x,y) data are interpreted. In a
	 * {@link ChartType#CategoryChart CategoryChart}, the X values are
	 * categories, and these are plotted consecutively, evenly spaced, and in
	 * row order. In a {@link ChartType#ScatterPlot ScatterPlot}, the X values
	 * are interpreted numerically (as for Y values).
	 * <p>
	 * The default chart type is a {@link ChartType#CategoryChart CategoryChart}.
	 * <p>
	 * 
	 * @see WCartesianChart#getType()
	 * @see WAxis#setScale(AxisScale scale)
	 * @see WCartesianChart#getAxis(Axis axis)
	 */
	public void setType(ChartType type) {
		if (this.type_ != type) {
			this.type_ = type;
			this.xAxis_.axis.init(this.interface_, Axis.XAxis);
			this.update();
		}
	}

	/**
	 * Returns the chart type.
	 * <p>
	 * 
	 * @see WCartesianChart#setType(ChartType type)
	 */
	public ChartType getType() {
		return this.type_;
	}

	/**
	 * Sets the chart orientation.
	 * <p>
	 * Sets the chart orientation, which corresponds to the orientation of the Y
	 * axis: a {@link Orientation#Vertical} orientation corresponds to the
	 * conventional way of a horizontal X axis and vertical Y axis. A
	 * {@link Orientation#Horizontal} orientation is the other way around.
	 * <p>
	 * The default orientation is {@link Orientation#Vertical}.
	 * <p>
	 * 
	 * @see WCartesianChart#getOrientation()
	 */
	public void setOrientation(Orientation orientation) {
		if (this.orientation_ != orientation) {
			this.orientation_ = orientation;
			this.update();
		}
	}

	/**
	 * Returns the chart orientation.
	 * <p>
	 * 
	 * @see WCartesianChart#setOrientation(Orientation orientation)
	 */
	public Orientation getOrientation() {
		return this.orientation_;
	}

	/**
	 * Sets the the model column for the X series.
	 * <p>
	 * Use this method to specify the default data for the X series. For a
	 * {@link ChartType#ScatterPlot ScatterPlot} this is mandatory if an X
	 * series is not specified for every WDataSeries. For a
	 * {@link ChartType#CategoryChart CategoryChart}, if not specified, an
	 * increasing series of integer numbers will be used (1, 2, ...).
	 * <p>
	 * Scatterplot dataseries may each individually be given its own X series
	 * data using {@link WDataSeries#setXSeriesColumn(int modelColumn)
	 * WDataSeries#setXSeriesColumn()}
	 * <p>
	 * The default value is -1 (not specified).
	 * <p>
	 * The series column is reset to -1 when the model is set (or changed). Thus
	 * you need to set a model before configuring the series.
	 * <p>
	 * 
	 * @see WCartesianChart#XSeriesColumn()
	 */
	public void setXSeriesColumn(int modelColumn) {
		if (this.XSeriesColumn_ != modelColumn) {
			this.XSeriesColumn_ = modelColumn;
			this.update();
		}
	}

	/**
	 * set the pen used to render the labels
	 * <p>
	 * This method overwrites the pen for all axes
	 * <p>
	 * 
	 * @see WAxis#setTextPen(WPen pen)
	 */
	public void setTextPen(final WPen pen) {
		if (pen.equals(this.textPen_)) {
			return;
		}
		this.textPen_ = pen;
		this.xAxis_.axis.setTextPen(pen);
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).axis.setTextPen(pen);
		}
	}

	/**
	 * Returns the model column for the X series.
	 * <p>
	 * 
	 * @see WCartesianChart#setXSeriesColumn(int modelColumn)
	 */
	public int XSeriesColumn() {
		return this.XSeriesColumn_;
	}

	/**
	 * Adds a data series.
	 * <p>
	 * A single chart may display one or more data series. Each data series
	 * displays data from a single model column in the chart. Series are plotted
	 * in the order that they have been added to the chart.
	 * <p>
	 * The series column is reset to -1 when the model is set (or changed). Thus
	 * you need to set a model before configuring the series.
	 * <p>
	 * 
	 * @see WCartesianChart#removeSeries(int modelColumn)
	 * @see WCartesianChart#setSeries(List series)
	 */
	public void addSeries(WDataSeries series) {
		this.series_.add(series);
		this.series_.get(this.series_.size() - 1).setChart(this);
		if (series.getType() == SeriesType.LineSeries
				|| series.getType() == SeriesType.CurveSeries) {
			this.assignJSPathsForSeries(series);
			this.assignJSTransformsForSeries(series);
		}
		this.update();
	}

	/**
	 * Removes a data series.
	 * <p>
	 * This removes the first data series which plots the given
	 * <code>modelColumn</code>.
	 * <p>
	 * 
	 * @see WCartesianChart#addSeries(WDataSeries series)
	 * @see WCartesianChart#setSeries(List series)
	 * @deprecated Use {@link WCartesianChart#removeSeries(WDataSeries series)
	 *             removeSeries()} instead
	 */
	public void removeSeries(int modelColumn) {
		for (int i = 0; i < this.series_.size(); ++i) {
			if (this.series_.get(i).getModelColumn() == modelColumn) {
				this.removeSeries(this.series_.get(i));
				return;
			}
		}
	}

	/**
	 * Removes a data series.
	 * <p>
	 * This will disassociate the given series from any WAxisSliderWidgets.
	 * <p>
	 * 
	 * @see WCartesianChart#addSeries(WDataSeries series)
	 * @see WCartesianChart#setSeries(List series)
	 */
	public void removeSeries(WDataSeries series) {
		int index = this.getSeriesIndexOf(series);
		if (index != -1) {
			for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
				if (this.axisSliderWidgets_.get(i).getSeries() == series) {
					this.axisSliderWidgets_.get(i)
							.setSeries((WDataSeries) null);
				}
			}
			if (series.getType() == SeriesType.LineSeries
					|| series.getType() == SeriesType.CurveSeries) {
				this.freeJSPathsForSeries(series);
				this.freeJSTransformsForSeries(series);
			}
			;
			this.series_.remove(0 + index);
			this.update();
		}
	}

	/**
	 * Sets all data series.
	 * <p>
	 * Replaces the current list of series with the new list.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>All series currently associated with the chart will be
	 * deleted. Hence, these series should be distinct from the currently
	 * associated series of this chart!</i>
	 * </p>
	 * 
	 * @see WCartesianChart#getSeries(int modelColumn)
	 * @see WCartesianChart#addSeries(WDataSeries series)
	 * @see WCartesianChart#removeSeries(WDataSeries series)
	 */
	public void setSeries(final List<WDataSeries> series) {
		for (int i = 0; i < this.series_.size(); ++i) {
			;
		}
		Utils.copyList(series, this.series_);
		this.freeJSHandlesForAllSeries();
		this.assignJSHandlesForAllSeries();
		for (int i = 0; i < this.series_.size(); ++i) {
			this.series_.get(i).setChart(this);
		}
		this.update();
	}

	/**
	 * Returns a data series corresponding to a data column.
	 * <p>
	 * Returns a reference to the first data series that plots data from
	 * <code>modelColumn</code>.
	 */
	public WDataSeries getSeries(int modelColumn) {
		int index = this.getSeriesIndexOf(modelColumn);
		if (index != -1) {
			return this.series_.get(index);
		}
		throw new WException("Column " + String.valueOf(modelColumn)
				+ " not in plot");
	}

	/**
	 * Returns a list with the current data series.
	 * <p>
	 * Returns the complete list of current data series.
	 * <p>
	 */
	public List<WDataSeries> getSeries() {
		return this.series_;
	}

	/**
	 * Returns a chart axis.
	 * <p>
	 * Returns a reference to the specified <code>axis</code>.
	 */
	public WAxis getAxis(Axis axis) {
		if (axis == Axis.XAxis) {
			return this.xAxis_.axis;
		} else {
			return this.yAxis(axis == Axis.Y1Axis ? 0 : 1);
		}
	}

	/**
	 * Sets an axis.
	 * <p>
	 * 
	 * @see WCartesianChart#getAxis(Axis axis)
	 */
	public void setAxis(WAxis waxis, Axis axis) {
		if (this.getAxis(axis) == waxis) {
			return;
		}
		if (axis == Axis.XAxis) {
			;
			this.xAxis_.axis = waxis;
			this.xAxis_.axis.init(this.interface_, axis);
		} else {
			int yIndex = axis == Axis.Y1Axis ? 0 : 1;
			;
			this.yAxes_.get(yIndex).axis = waxis;
			this.yAxes_.get(yIndex).axis.init(this.interface_, axis);
		}
	}

	/**
	 * Returns a vector of all Y axes associated with this chart.
	 * <p>
	 * This defaults to a vector of two axes: the Y1 and Y2 axes. Y1 will be at
	 * index 0, and Y2 will be at index 1.
	 */
	public List<WAxis> getYAxes() {
		List<WAxis> result = new ArrayList<WAxis>();
		;
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			result.add(this.yAxes_.get(i).axis);
		}
		return result;
	}

	/**
	 * Returns the number of Y axes associated with this chart.
	 */
	public int getYAxisCount() {
		return (int) this.yAxes_.size();
	}

	/**
	 * Retrieves the Y axis at index i.
	 * <p>
	 * The following expressions are always true:
	 * <p>
	 * 
	 * <pre>
	 *   {@code
	 *    axis(Y1) == yAxis(0)
	 *    axis(Y2) == yAxis(1)
	 *   }
	 * </pre>
	 */
	public WAxis yAxis(int i) {
		return this.yAxes_.get(i).axis;
	}

	/**
	 * Adds a Y axis to this chart.
	 * <p>
	 * The first extra axis will have index 2, the next index 3,...
	 * <p>
	 * Returns the index of the added axis.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>This transfers ownership of the given {@link WAxis} to
	 * this chart. </i>
	 * </p>
	 */
	public int addYAxis(WAxis waxis) {
		int idx = (int) this.yAxes_.size();
		this.yAxes_.add(new WCartesianChart.AxisStruct(waxis));
		this.yAxes_.get(idx).axis.initYAxis(this.interface_, idx);
		this.yAxes_.get(idx).axis.setPadding(this.getAxisPadding());
		this.yAxes_.get(idx).axis.setSoftLabelClipping(true);
		this.yAxes_.get(idx).transformHandle = this.createJSTransform();
		this.yAxes_.get(idx).transformChanged = new JSignal(this,
				"yTransformChanged" + String.valueOf(idx));
		this.update();
		return idx;
	}

	/**
	 * Removes the Y axis with the given id.
	 * <p>
	 * The indices of the axes with an id higher than yAxisId will be
	 * decremented.
	 * <p>
	 * Any {@link WDataSeries} associated with the removed axis are also
	 * removed.
	 */
	public void removeYAxis(int yAxisId) {
		{
			int i = 0;
			while (i < this.series_.size()) {
				if (this.series_.get(i).getYAxis() == yAxisId) {
					this.removeSeries(this.series_.get(i));
				} else {
					if (this.series_.get(i).getYAxis() > yAxisId) {
						this.series_.get(i).bindToYAxis(
								this.series_.get(i).getYAxis() - 1);
					}
					++i;
				}
			}
		}
		if (this.getCrosshairYAxis() > yAxisId) {
			this.setCrosshairYAxis(this.getCrosshairYAxis() - 1);
		}
		this.clearPensForAxis(Axis.YAxis, yAxisId);
		;
		this.yAxes_.remove(0 + yAxisId);
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).axis.yAxis_ = (int) i;
			this.yAxes_.get(i).axis.axis_ = i == 1 ? Axis.Y2Axis : Axis.Y1Axis;
		}
		this.update();
	}

	/**
	 * Clears all Y axes.
	 * <p>
	 * The effect is the same as repeatedly using
	 * {@link WCartesianChart#removeYAxis(int yAxisId) removeYAxis()} until are
	 * axes are removed, i.e. any {@link WDataSeries} will also be removed.
	 */
	public void clearYAxes() {
		while (!this.series_.isEmpty()) {
			this.removeSeries(this.series_.get(this.series_.size() - 1));
		}
		this.clearPens();
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			;
		}
		this.yAxes_.clear();
		this.update();
	}

	/**
	 * Sets the margin between bars of different series.
	 * <p>
	 * Use this method to change the margin that is set between bars of
	 * different series. The margin is specified as a fraction of the width. For
	 * example, a value of 0.1 adds a 10% margin between bars of each series.
	 * Negative values are also allowed. For example, use a margin of -1 to plot
	 * the bars of different series on top of each other.
	 * <p>
	 * The default value is 0.
	 */
	public void setBarMargin(double margin) {
		if (this.barMargin_ != margin) {
			this.barMargin_ = margin;
			this.update();
		}
	}

	/**
	 * Returns the margin between bars of different series.
	 * <p>
	 * 
	 * @see WCartesianChart#setBarMargin(double margin)
	 */
	public double getBarMargin() {
		return this.barMargin_;
	}

	/**
	 * Enables the legend.
	 * <p>
	 * The location of the legend can be configured using
	 * {@link WCartesianChart#setLegendLocation(LegendLocation location, Side side, AlignmentFlag alignment)
	 * setLegendLocation()}. Only series for which the legend is enabled are
	 * included in this legend.
	 * <p>
	 * The default value is <code>false</code>.
	 * <p>
	 * 
	 * @see WDataSeries#isLegendEnabled()
	 * @see WCartesianChart#setLegendLocation(LegendLocation location, Side
	 *      side, AlignmentFlag alignment)
	 */
	public void setLegendEnabled(boolean enabled) {
		this.legend_.setLegendEnabled(enabled);
		this.update();
	}

	/**
	 * Returns whether the legend is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendEnabled(boolean enabled)
	 */
	public boolean isLegendEnabled() {
		return this.legend_.isLegendEnabled();
	}

	/**
	 * Configures the legend location.
	 * <p>
	 * The legend can be renderd either inside or outside of the chart area.
	 * When <code>location</code> is {@link LegendLocation#LegendInside
	 * Chart::LegendInside}, the legend will be rendered inside the chart. When
	 * <code>location</code> is {@link LegendLocation#LegendOutside
	 * Chart::Legendoutside}, the legend is rendered outside the chart, in the
	 * chart padding area.
	 * <p>
	 * The provided <code>side</code> can either be {@link Side#Left},
	 * {@link Side#Right}, {@link Side#Top}, {@link Side#Bottom} and configures
	 * the side of the chart at which the legend is displayed.
	 * <p>
	 * The <code>alignment</code> specifies how the legend is aligned. This can
	 * be a horizontal alignment flag ({@link AlignmentFlag#AlignLeft},
	 * {@link AlignmentFlag#AlignCenter}, or {@link AlignmentFlag#AlignRight}),
	 * when the <code>side</code> is Bottom or Top, or a vertical alignment flag
	 * ({@link AlignmentFlag#AlignTop}, {@link AlignmentFlag#AlignMiddle}, or
	 * {@link AlignmentFlag#AlignBottom}) when the <code>side</code> is Left or
	 * Right.
	 * <p>
	 * The default location is {@link LegendLocation#LegendOutside
	 * Chart::LegendOutside}, {@link Side#Right} and
	 * {@link AlignmentFlag#AlignMiddle}.
	 * <p>
	 * To have more control over the legend, you could reimplement the
	 * {@link WCartesianChart#renderLegendItem(WPainter painter, WPointF pos, WDataSeries series)
	 * renderLegendItem()} method to customize how one item in the legend is
	 * rendered, or, alternatively you can disable the legend generated by the
	 * chart itself, and reimplement the
	 * {@link WCartesianChart#paint(WPainter painter, WRectF rectangle) paint()}
	 * method in which you use the
	 * {@link WCartesianChart#renderLegendItem(WPainter painter, WPointF pos, WDataSeries series)
	 * renderLegendItem()} method repeatedly to render a customized legend.
	 * <p>
	 * 
	 * @see WDataSeries#setLegendEnabled(boolean enabled)
	 */
	public void setLegendLocation(LegendLocation location, Side side,
			AlignmentFlag alignment) {
		this.legend_.setLegendLocation(location, side, alignment);
		this.update();
	}

	/**
	 * Configures the legend decoration.
	 * <p>
	 * This configures the font, border and background for the legend.
	 * <p>
	 * The default font is a 10pt sans serif font (the same as the default axis
	 * label font), the default <code>border</code> is {@link PenStyle#NoPen
	 * NoPen} and the default <code>background</code> is
	 * {@link BrushStyle#NoBrush NoBrush}.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendEnabled(boolean enabled)
	 */
	public void setLegendStyle(final WFont font, final WPen border,
			final WBrush background) {
		this.legend_.setLegendStyle(font, border, background);
		this.update();
	}

	/**
	 * Returns the legend location.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendLocation(LegendLocation location, Side
	 *      side, AlignmentFlag alignment)
	 */
	public LegendLocation getLegendLocation() {
		return this.legend_.getLegendLocation();
	}

	/**
	 * Returns the legend side.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendLocation(LegendLocation location, Side
	 *      side, AlignmentFlag alignment)
	 */
	public Side getLegendSide() {
		return this.legend_.getLegendSide();
	}

	/**
	 * Returns the legend alignment.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendLocation(LegendLocation location, Side
	 *      side, AlignmentFlag alignment)
	 */
	public AlignmentFlag getLegendAlignment() {
		return this.legend_.getLegendAlignment();
	}

	/**
	 * Returns the legend columns.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendColumns(int columns, WLength columnWidth)
	 */
	public int getLegendColumns() {
		return this.legend_.getLegendColumns();
	}

	/**
	 * Returns the legend column width.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendColumns(int columns, WLength columnWidth)
	 */
	public WLength getLegendColumnWidth() {
		return this.legend_.getLegendColumnWidth();
	}

	/**
	 * Returns the legend font.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush
	 *      background)
	 */
	public WFont getLegendFont() {
		return this.legend_.getLegendFont();
	}

	/**
	 * Returns the legend border pen.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush
	 *      background)
	 */
	public WPen getLegendBorder() {
		return this.legend_.getLegendBorder();
	}

	/**
	 * Returns the legend background brush.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendStyle(WFont font, WPen border, WBrush
	 *      background)
	 */
	public WBrush getLegendBackground() {
		return this.legend_.getLegendBackground();
	}

	/**
	 * Configures multiple legend columns.
	 * <p>
	 * Multiple columns are typically useful when placing the legend at the top
	 * or at the bottom of the chart.
	 * <p>
	 * The default value is a single column, 100 pixels wide.
	 * <p>
	 * When automatic chart layout is enabled, then the legend column width is
	 * computed automatically, and this setting is ignored.
	 * <p>
	 * 
	 * @see WAbstractChart#setAutoLayoutEnabled(boolean enabled)
	 */
	public void setLegendColumns(int columns, final WLength columnWidth) {
		this.legend_.setLegendColumns(columns);
		this.legend_.setLegendColumnWidth(columnWidth);
		this.update();
	}

	public void paint(final WPainter painter, final WRectF rectangle) {
		while (!this.getAreas().isEmpty()) {
			if ((this).getAreas().get(0) != null)
				(this).getAreas().get(0).remove();
		}
		if (!painter.isActive()) {
			throw new WException(
					"WCartesianChart::paint(): painter is not active.");
		}
		WRectF rect = rectangle;
		if ((rect == null) || rect.isEmpty()) {
			rect = painter.getWindow();
		}
		this.render(painter, rect);
	}

	/**
	 * Draws the marker for a given data series.
	 * <p>
	 * Draws the marker for the indicated <code>series</code> in the
	 * <code>result</code>. This method is called while painting the chart, and
	 * you may want to reimplement this method if you wish to provide a custom
	 * marker for a particular data series.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendEnabled(boolean enabled)
	 */
	public void drawMarker(final WDataSeries series, final WPainterPath result) {
		final double size = 6.0;
		final double hsize = size / 2;
		switch (series.getMarker()) {
		case CircleMarker:
			result.addEllipse(-hsize, -hsize, size, size);
			break;
		case SquareMarker:
			result.addRect(new WRectF(-hsize, -hsize, size, size));
			break;
		case CrossMarker:
			result.moveTo(-1.3 * hsize, 0);
			result.lineTo(1.3 * hsize, 0);
			result.moveTo(0, -1.3 * hsize);
			result.lineTo(0, 1.3 * hsize);
			break;
		case XCrossMarker:
			result.moveTo(-hsize, -hsize);
			result.lineTo(hsize, hsize);
			result.moveTo(-hsize, hsize);
			result.lineTo(hsize, -hsize);
			break;
		case TriangleMarker:
			result.moveTo(0, 0.6 * hsize);
			result.lineTo(-hsize, 0.6 * hsize);
			result.lineTo(0, -hsize);
			result.lineTo(hsize, 0.6 * hsize);
			result.closeSubPath();
			break;
		case StarMarker: {
			double angle = 3.14159265358979323846 / 2.0;
			for (int i = 0; i < 5; ++i) {
				double x = Math.cos(angle) * hsize;
				double y = -Math.sin(angle) * hsize;
				result.moveTo(0, 0);
				result.lineTo(x, y);
				angle += 3.14159265358979323846 * 2.0 / 5.0;
			}
		}
			break;
		case InvertedTriangleMarker:
			result.moveTo(0, -0.6 * hsize);
			result.lineTo(-hsize, -0.6 * hsize);
			result.lineTo(0, hsize);
			result.lineTo(hsize, -0.6 * hsize);
			result.closeSubPath();
			break;
		case DiamondMarker: {
			double s = Math.sqrt(2.0) * hsize;
			result.moveTo(0, s);
			result.lineTo(s, 0);
			result.lineTo(0, -s);
			result.lineTo(-s, 0);
			result.closeSubPath();
		}
			break;
		case AsteriskMarker: {
			double angle = 3.14159265358979323846 / 2.0;
			for (int i = 0; i < 6; ++i) {
				double x = Math.cos(angle) * hsize;
				double y = -Math.sin(angle) * hsize;
				result.moveTo(0, 0);
				result.lineTo(x, y);
				angle += 3.14159265358979323846 / 3.0;
			}
		}
			break;
		case CustomMarker:
			result.assign(series.getCustomMarker());
			break;
		default:
			;
		}
	}

	/**
	 * Renders the legend icon for a given data series.
	 * <p>
	 * Renders the legend icon for the indicated <code>series</code> in the
	 * <code>painter</code> at position <code>pos</code>.
	 * <p>
	 * This method is called while rendering a legend item, and you may want to
	 * reimplement this method if you wish to provide a custom legend icon for a
	 * particular data series.
	 * <p>
	 * 
	 * @see WCartesianChart#renderLegendItem(WPainter painter, WPointF pos,
	 *      WDataSeries series)
	 */
	public void renderLegendIcon(final WPainter painter, final WPointF pos,
			final WDataSeries series) {
		WShadow shadow = painter.getShadow();
		switch (series.getType()) {
		case BarSeries: {
			WPainterPath path = new WPainterPath();
			path.moveTo(-6, 8);
			path.lineTo(-6, -8);
			path.lineTo(6, -8);
			path.lineTo(6, 8);
			painter.translate(pos.getX() + 7.5, pos.getY());
			painter.setShadow(series.getShadow());
			painter.fillPath(path, series.getBrush());
			painter.setShadow(shadow);
			painter.strokePath(path, series.getPen());
			painter.translate(-(pos.getX() + 7.5), -pos.getY());
			break;
		}
		case LineSeries:
		case CurveSeries: {
			painter.setPen(series.getPen().clone());
			double offset = series.getPen().getWidth().equals(new WLength(0)) ? 0.5
					: 0;
			painter.setShadow(series.getShadow());
			painter.drawLine(pos.getX(), pos.getY() + offset, pos.getX() + 16,
					pos.getY() + offset);
			painter.setShadow(shadow);
		}
		case PointSeries: {
			WPainterPath path = new WPainterPath();
			this.drawMarker(series, path);
			if (!path.isEmpty()) {
				painter.translate(pos.getX() + 8, pos.getY());
				painter.setShadow(series.getShadow());
				painter.fillPath(path, series.getMarkerBrush());
				painter.setShadow(shadow);
				painter.strokePath(path, series.getMarkerPen());
				painter.translate(-(pos.getX() + 8), -pos.getY());
			}
			break;
		}
		}
	}

	/**
	 * Renders the legend item for a given data series.
	 * <p>
	 * Renders the legend item for the indicated <code>series</code> in the
	 * <code>painter</code> at position <code>pos</code>. The default
	 * implementation draws the marker, and the series description to the right.
	 * The series description is taken from the model&apos;s header data for
	 * that series&apos; data column.
	 * <p>
	 * This method is called while painting the chart, and you may want to
	 * reimplement this method if you wish to provide a custom marker for a
	 * particular data series.
	 * <p>
	 * 
	 * @see WCartesianChart#setLegendEnabled(boolean enabled)
	 */
	public void renderLegendItem(final WPainter painter, final WPointF pos,
			final WDataSeries series) {
		WPen fontPen = painter.getPen();
		this.renderLegendIcon(painter, pos, series);
		painter.setPen(fontPen.clone());
		int width = (int) this.getLegendColumnWidth().toPixels();
		if (width < 100) {
			width = 100;
		}
		painter.drawText(pos.getX() + 23, pos.getY() - 9, width, 20,
				EnumSet.of(AlignmentFlag.AlignLeft, AlignmentFlag.AlignMiddle),
				series.getModel().getHeaderData(series.getModelColumn()));
	}

	/**
	 * Maps from device coordinates to model coordinates.
	 * <p>
	 * Maps a position in the chart back to model coordinates.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * If the chart is interactive, mapFromDevice will correctly take the
	 * current zoom range into account.
	 * <p>
	 * 
	 * @see WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis,
	 *      int xSegment, int ySegment)
	 */
	public WPointF mapFromDevice(final WPointF point, Axis ordinateAxis) {
		return this.mapFromDevice(point, ordinateAxis == Axis.Y1Axis ? 0 : 1);
	}

	/**
	 * Maps from device coordinates to model coordinates.
	 * <p>
	 * Returns {@link #mapFromDevice(WPointF point, Axis ordinateAxis)
	 * mapFromDevice(point, Axis.OrdinateAxis)}
	 */
	public final WPointF mapFromDevice(final WPointF point) {
		return mapFromDevice(point, Axis.OrdinateAxis);
	}

	/**
	 * Maps from device coordinates to model coordinates.
	 * <p>
	 * Maps a position in the chart back to model coordinates.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * If the chart is interactive, mapFromDevice will correctly take the
	 * current zoom range into account.
	 * <p>
	 * 
	 * @see WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis,
	 *      int xSegment, int ySegment)
	 */
	public WPointF mapFromDevice(final WPointF point, int ordinateAxis) {
		if (this.isInteractive()) {
			return this.mapFromDeviceWithoutTransform(
					this.zoomRangeTransform(
							this.xAxis_.transformHandle.getValue(),
							this.yAxes_.get(ordinateAxis).transformHandle
									.getValue()).getInverted().map(point),
					ordinateAxis);
		} else {
			return this.mapFromDeviceWithoutTransform(point, ordinateAxis);
		}
	}

	/**
	 * Maps from device coordinates to model coordinates, ignoring the current
	 * zoom range.
	 * <p>
	 * Maps a position in the chart back to model coordinates, as if the chart
	 * was not zoomed in (nor panned).
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * This function will not take the current zoom range into account. The
	 * mapping will be performed as if
	 * {@link WCartesianChart#zoomRangeTransform(int yAxis)
	 * zoomRangeTransform()} is the identity transform.
	 * <p>
	 * 
	 * @see WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object
	 *      yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 */
	public WPointF mapFromDeviceWithoutTransform(final WPointF point,
			Axis ordinateAxis) {
		return this.mapFromDeviceWithoutTransform(point,
				ordinateAxis == Axis.Y1Axis ? 0 : 1);
	}

	/**
	 * Maps from device coordinates to model coordinates, ignoring the current
	 * zoom range.
	 * <p>
	 * Returns
	 * {@link #mapFromDeviceWithoutTransform(WPointF point, Axis ordinateAxis)
	 * mapFromDeviceWithoutTransform(point, Axis.OrdinateAxis)}
	 */
	public final WPointF mapFromDeviceWithoutTransform(final WPointF point) {
		return mapFromDeviceWithoutTransform(point, Axis.OrdinateAxis);
	}

	/**
	 * Maps from device coordinates to model coordinates, ignoring the current
	 * zoom range.
	 * <p>
	 * Maps a position in the chart back to model coordinates, as if the chart
	 * was not zoomed in (nor panned).
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * This function will not take the current zoom range into account. The
	 * mapping will be performed as if
	 * {@link WCartesianChart#zoomRangeTransform(int yAxis)
	 * zoomRangeTransform()} is the identity transform.
	 * <p>
	 * 
	 * @see WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object
	 *      yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 */
	public WPointF mapFromDeviceWithoutTransform(final WPointF point,
			int ordinateAxis) {
		final WAxis xAxis = this.getAxis(Axis.XAxis);
		final WAxis yAxis = this.yAxis(ordinateAxis);
		WPointF p = this.inverseHv(point.getX(), point.getY(), this.getWidth()
				.toPixels());
		return new WPointF(xAxis.mapFromDevice(p.getX()
				- this.chartArea_.getLeft()),
				yAxis.mapFromDevice(this.chartArea_.getBottom() - p.getY()));
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * This returns the chart device coordinates for a (x,y) pair of model
	 * values.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * The <code>xSegment</code> and <code>ySegment</code> arguments are
	 * relevant only when the corresponding axis is broken using
	 * {@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()}.
	 * Then, its possible values may be 0 (below the break) or 1 (above the
	 * break).
	 * <p>
	 * If the chart is interactive, mapToDevice will correctly take the current
	 * zoom range into account.
	 * <p>
	 * 
	 * @see WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
	 */
	public WPointF mapToDevice(final Object xValue, final Object yValue,
			Axis axis, int xSegment, int ySegment) {
		return this.mapToDevice(xValue, yValue, axis == Axis.Y1Axis ? 0 : 1,
				xSegment, ySegment);
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * Returns
	 * {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int ySegment)
	 * mapToDevice(xValue, yValue, Axis.OrdinateAxis, 0, 0)}
	 */
	public final WPointF mapToDevice(final Object xValue, final Object yValue) {
		return mapToDevice(xValue, yValue, Axis.OrdinateAxis, 0, 0);
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * Returns
	 * {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int ySegment)
	 * mapToDevice(xValue, yValue, axis, 0, 0)}
	 */
	public final WPointF mapToDevice(final Object xValue, final Object yValue,
			Axis axis) {
		return mapToDevice(xValue, yValue, axis, 0, 0);
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * Returns
	 * {@link #mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int ySegment)
	 * mapToDevice(xValue, yValue, axis, xSegment, 0)}
	 */
	public final WPointF mapToDevice(final Object xValue, final Object yValue,
			Axis axis, int xSegment) {
		return mapToDevice(xValue, yValue, axis, xSegment, 0);
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * This returns the chart device coordinates for a (x,y) pair of model
	 * values.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * The <code>xSegment</code> and <code>ySegment</code> arguments are
	 * relevant only when the corresponding axis is broken using
	 * {@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()}.
	 * Then, its possible values may be 0 (below the break) or 1 (above the
	 * break).
	 * <p>
	 * If the chart is interactive, mapToDevice will correctly take the current
	 * zoom range into account.
	 * <p>
	 * 
	 * @see WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
	 */
	public WPointF mapToDevice(final Object xValue, final Object yValue,
			int axis, int xSegment, int ySegment) {
		if (this.isInteractive()) {
			return this.zoomRangeTransform(
					this.xAxis_.transformHandle.getValue(),
					this.yAxes_.get(axis).transformHandle.getValue()).map(
					this.mapToDeviceWithoutTransform(xValue, yValue, axis,
							xSegment, ySegment));
		} else {
			return this.mapToDeviceWithoutTransform(xValue, yValue, axis,
					xSegment, ySegment);
		}
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * Returns
	 * {@link #mapToDevice(Object xValue, Object yValue, int axis, int xSegment, int ySegment)
	 * mapToDevice(xValue, yValue, axis, 0, 0)}
	 */
	public final WPointF mapToDevice(final Object xValue, final Object yValue,
			int axis) {
		return mapToDevice(xValue, yValue, axis, 0, 0);
	}

	/**
	 * Maps model values onto chart coordinates.
	 * <p>
	 * Returns
	 * {@link #mapToDevice(Object xValue, Object yValue, int axis, int xSegment, int ySegment)
	 * mapToDevice(xValue, yValue, axis, xSegment, 0)}
	 */
	public final WPointF mapToDevice(final Object xValue, final Object yValue,
			int axis, int xSegment) {
		return mapToDevice(xValue, yValue, axis, xSegment, 0);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * This returns the chart device coordinates for a (x,y) pair of model
	 * values.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * The <code>xSegment</code> and <code>ySegment</code> arguments are
	 * relevant only when the corresponding axis is broken using
	 * {@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()}.
	 * Then, its possible values may be 0 (below the break) or 1 (above the
	 * break).
	 * <p>
	 * This function will not take the current zoom range into account.The
	 * mapping will be performed as if
	 * {@link WCartesianChart#zoomRangeTransform(int yAxis)
	 * zoomRangeTransform()} is the identity transform.
	 * <p>
	 * 
	 * @see WCartesianChart#mapFromDeviceWithoutTransform(WPointF point, Axis
	 *      ordinateAxis)
	 */
	public WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, Axis ordinateAxis, int xSegment, int ySegment) {
		return this.mapToDeviceWithoutTransform(xValue, yValue,
				ordinateAxis == Axis.Y1Axis ? 0 : 1, xSegment, ySegment);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * Returns
	 * {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform(xValue, yValue, Axis.OrdinateAxis, 0, 0)}
	 */
	public final WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue) {
		return mapToDeviceWithoutTransform(xValue, yValue, Axis.OrdinateAxis,
				0, 0);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * Returns
	 * {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0)}
	 */
	public final WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, Axis ordinateAxis) {
		return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * Returns
	 * {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment, 0)}
	 */
	public final WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, Axis ordinateAxis, int xSegment) {
		return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis,
				xSegment, 0);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * This returns the chart device coordinates for a (x,y) pair of model
	 * values.
	 * <p>
	 * This uses the axis dimensions that are based on the latest chart
	 * rendering. If you have not yet rendered the chart, or wish that the
	 * mapping already reflects model changes since the last rendering, you
	 * should call
	 * {@link WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout()} first.
	 * <p>
	 * The <code>xSegment</code> and <code>ySegment</code> arguments are
	 * relevant only when the corresponding axis is broken using
	 * {@link WAxis#setBreak(double minimum, double maximum) WAxis#setBreak()}.
	 * Then, its possible values may be 0 (below the break) or 1 (above the
	 * break).
	 * <p>
	 * This function will not take the current zoom range into account.The
	 * mapping will be performed as if
	 * {@link WCartesianChart#zoomRangeTransform(int yAxis)
	 * zoomRangeTransform()} is the identity transform.
	 * <p>
	 * 
	 * @see WCartesianChart#mapFromDeviceWithoutTransform(WPointF point, Axis
	 *      ordinateAxis)
	 */
	public WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, int ordinateAxis, int xSegment, int ySegment) {
		final WAxis xAxis = this.getAxis(Axis.XAxis);
		final WAxis yAxis = this.yAxis(ordinateAxis);
		double x = this.chartArea_.getLeft()
				+ xAxis.mapToDevice(xValue, xSegment);
		double y = this.chartArea_.getBottom()
				- yAxis.mapToDevice(yValue, ySegment);
		return this.hv(x, y, this.getWidth().toPixels());
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * Returns
	 * {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, int ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0)}
	 */
	public final WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, int ordinateAxis) {
		return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, 0, 0);
	}

	/**
	 * Maps model values onto chart coordinates, ignoring the current zoom
	 * range.
	 * <p>
	 * Returns
	 * {@link #mapToDeviceWithoutTransform(Object xValue, Object yValue, int ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis, xSegment, 0)}
	 */
	public final WPointF mapToDeviceWithoutTransform(final Object xValue,
			final Object yValue, int ordinateAxis, int xSegment) {
		return mapToDeviceWithoutTransform(xValue, yValue, ordinateAxis,
				xSegment, 0);
	}

	/**
	 * Initializes the chart layout.
	 * <p>
	 * The mapping between model and device coordinates is only established
	 * after a rendering phase, or after calling initLayout manually.
	 * <p>
	 * You need a layout in order to use the
	 * {@link WCartesianChart#mapFromDevice(WPointF point, Axis ordinateAxis)
	 * mapFromDevice()} and
	 * {@link WCartesianChart#mapToDevice(Object xValue, Object yValue, Axis axis, int xSegment, int ySegment)
	 * mapToDevice()} methods.
	 * <p>
	 * Unless a specific chart rectangle is specified, the entire widget area is
	 * assumed.
	 */
	public boolean initLayout(final WRectF rectangle, WPaintDevice device) {
		WRectF rect = rectangle;
		if ((rect == null) || rect.isEmpty()) {
			rect = new WRectF(0.0, 0.0, this.getWidth().toPixels(), this
					.getHeight().toPixels());
		}
		if (this.getOrientation() == Orientation.Vertical) {
			this.width_ = (int) rect.getWidth();
			this.height_ = (int) rect.getHeight();
		} else {
			this.width_ = (int) rect.getHeight();
			this.height_ = (int) rect.getWidth();
		}
		this.xAxis_.location.loc = AxisValue.MinimumValue;
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).location.loc = AxisValue.MinimumValue;
		}
		WPaintDevice d = device;
		if (!(d != null)) {
			d = this.getCreatePaintDevice();
		}
		boolean autoLayout = this.isAutoLayoutEnabled();
		if (autoLayout
				&& EnumUtils.mask(d.getFeatures(),
						WPaintDevice.FeatureFlag.HasFontMetrics).equals(0)) {
			logger.error(new StringWriter()
					.append("setAutoLayout(): device does not have font metrics (not even server-side font metrics).")
					.toString());
			autoLayout = false;
		}
		if (autoLayout) {
			WCartesianChart self = this;
			self.setPlotAreaPadding(40, EnumSet.of(Side.Left, Side.Right));
			self.setPlotAreaPadding(30, EnumSet.of(Side.Top, Side.Bottom));
			this.calcChartArea();
			this.xAxis_.transform.assign(new WTransform());
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				this.yAxes_.get(i).transform.assign(new WTransform());
			}
			if (this.chartArea_.getWidth() <= 5
					|| this.chartArea_.getHeight() <= 5
					|| !this.prepareAxes(device)) {
				if (this.isInteractive()) {
					this.xAxis_.transform.assign(this.xAxis_.transformHandle
							.getValue());
					for (int i = 0; i < this.getYAxisCount(); ++i) {
						this.yAxes_.get(i).transform
								.assign(this.yAxes_.get(i).transformHandle
										.getValue());
					}
				}
				return false;
			}
			{
				WMeasurePaintDevice md = new WMeasurePaintDevice(d);
				WPainter painter = new WPainter(md);
				this.renderAxes(painter,
						EnumSet.of(AxisProperty.Line, AxisProperty.Labels));
				this.renderLegend(painter);
				WRectF bounds = md.getBoundingRect();
				final int MARGIN = 5;
				int corrLeft = (int) Math.max(0.0,
						rect.getLeft() - bounds.getLeft() + MARGIN);
				int corrRight = (int) Math.max(0.0,
						bounds.getRight() - rect.getRight() + MARGIN);
				int corrTop = (int) Math.max(0.0,
						rect.getTop() - bounds.getTop() + MARGIN);
				int corrBottom = (int) Math.max(0.0,
						bounds.getBottom() - rect.getBottom() + MARGIN);
				self.setPlotAreaPadding(this.getPlotAreaPadding(Side.Left)
						+ corrLeft, EnumSet.of(Side.Left));
				self.setPlotAreaPadding(this.getPlotAreaPadding(Side.Right)
						+ corrRight, EnumSet.of(Side.Right));
				self.setPlotAreaPadding(this.getPlotAreaPadding(Side.Top)
						+ corrTop, EnumSet.of(Side.Top));
				self.setPlotAreaPadding(this.getPlotAreaPadding(Side.Bottom)
						+ corrBottom, EnumSet.of(Side.Bottom));
			}
		}
		if (!(device != null)) {
			;
		}
		this.calcChartArea();
		boolean result = this.chartArea_.getWidth() > 5
				&& this.chartArea_.getHeight() > 5 && this.prepareAxes(device);
		if (this.isInteractive()) {
			this.xAxis_.transform
					.assign(this.xAxis_.transformHandle.getValue());
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				this.yAxes_.get(i).transform
						.assign(this.yAxes_.get(i).transformHandle.getValue());
			}
		} else {
			this.xAxis_.transform.assign(new WTransform());
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				this.yAxes_.get(i).transform.assign(new WTransform());
			}
		}
		if (this.isInteractive()) {
			WCartesianChart self = this;
			self.clearPens();
			self.createPensForAxis(Axis.XAxis, -1);
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				self.createPensForAxis(Axis.YAxis, i);
			}
			if (this.curvePaths_.isEmpty()) {
				self.assignJSHandlesForAllSeries();
			}
		}
		return result;
	}

	/**
	 * Initializes the chart layout.
	 * <p>
	 * Returns {@link #initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout(null, (WPaintDevice)null)}
	 */
	public final boolean initLayout() {
		return initLayout(null, (WPaintDevice) null);
	}

	/**
	 * Initializes the chart layout.
	 * <p>
	 * Returns {@link #initLayout(WRectF rectangle, WPaintDevice device)
	 * initLayout(rectangle, (WPaintDevice)null)}
	 */
	public final boolean initLayout(final WRectF rectangle) {
		return initLayout(rectangle, (WPaintDevice) null);
	}

	/**
	 * Creates a widget which renders the a legend item.
	 * <p>
	 * The legend item widget will contain a text and a {@link WPaintedWidget}
	 * which draws the series&apos; symbol.
	 */
	public WWidget createLegendItemWidget(int index) {
		WContainerWidget legendItem = new WContainerWidget();
		legendItem.addWidget(new WCartesianChart.IconWidget(this, index));
		WText label = new WText(this.getSeries(index).getModel()
				.getHeaderData(index));
		label.setVerticalAlignment(AlignmentFlag.AlignTop);
		legendItem.addWidget(label);
		return legendItem;
	}

	/**
	 * Adds a data point area (used for displaying e.g. tooltips).
	 * <p>
	 * You may want to specialize this is if you wish to modify (or delete) the
	 * area.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>Currently, an area is only created if the
	 * {@link ItemDataRole#ToolTipRole} data at the data point is not empty.
	 * </i>
	 * </p>
	 */
	public void addDataPointArea(final WDataSeries series, int xRow,
			int xColumn, WAbstractArea area) {
		if (this.getAreas().isEmpty()) {
			this.addAreaMask();
		}
		this.addArea(area);
	}

	/**
	 * Sets the padding between the chart area and the axes.
	 * <p>
	 * This calls WAxes::setPadding() on all axes.
	 * <p>
	 * 
	 * @see WCartesianChart#getAxisPadding()
	 */
	public void setAxisPadding(int padding) {
		this.axisPadding_ = padding;
		this.xAxis_.axis.setPadding(padding);
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).axis.setPadding(padding);
		}
	}

	/**
	 * Returns the padding between the chart area and the axes.
	 * <p>
	 * This number may not reflect the actual padding of the individual axes, if
	 * another padding has been applied on the individual axes.
	 * <p>
	 * 
	 * @see WCartesianChart#setAxisPadding(int padding)
	 */
	public int getAxisPadding() {
		return this.axisPadding_;
	}

	/**
	 * Sets the pen of the border to be drawn around the chart area.
	 * <p>
	 * 
	 * @see WCartesianChart#getBorderPen()
	 */
	public void setBorderPen(final WPen pen) {
		if (!this.borderPen_.equals(pen)) {
			this.borderPen_ = pen;
			this.update();
		}
	}

	/**
	 * Returns the pen used to draw the border around the chart area.
	 * <p>
	 * Defaults to NoPen.
	 * <p>
	 * 
	 * @see WCartesianChart#setBorderPen(WPen pen)
	 */
	public WPen getBorderPen() {
		return this.borderPen_;
	}

	/**
	 * Add a curve label.
	 * <p>
	 * 
	 * @see CurveLabel#CurveLabel(WDataSeries series, WPointF point, String
	 *      label)
	 */
	public void addCurveLabel(final CurveLabel label) {
		this.curveLabels_.add(label);
		this.update();
	}

	/**
	 * Configure all curve labels at once.
	 * <p>
	 * 
	 * @see WCartesianChart#addCurveLabel(CurveLabel label)
	 */
	public void setCurveLabels(final List<CurveLabel> labels) {
		Utils.copyList(labels, this.curveLabels_);
		this.update();
	}

	/**
	 * Clear all curve labels.
	 * <p>
	 * 
	 * @see WCartesianChart#addCurveLabel(CurveLabel label)
	 */
	public void clearCurveLabels() {
		this.curveLabels_.clear();
		this.update();
	}

	/**
	 * Get all of the registered curve labels.
	 * <p>
	 * 
	 * @see WCartesianChart#setCurveLabels(List labels)
	 */
	public List<CurveLabel> getCurveLabels() {
		return this.curveLabels_;
	}

	/**
	 * Returns whether this chart is interactive.
	 * <p>
	 * Return true iff one of the interactive features is enabled, and the chart
	 * is being rendered on an HTML canvas.
	 */
	boolean isInteractive() {
		return !this.yAxes_.isEmpty()
				&& (this.zoomEnabled_ || this.panEnabled_
						|| this.crosshairEnabled_ || this.followCurve_ != null
						|| this.axisSliderWidgets_.size() > 0
						|| this.seriesSelectionEnabled_ || this.curveManipulationEnabled_)
				&& this.getMethod() == WPaintedWidget.Method.HtmlCanvas;
	}

	/**
	 * Enables zoom functionality.
	 * <p>
	 * When using the mouse, press the ctrl key while scrolling to zoom in/out a
	 * specific point on the chart. If you press shift+ctrl, it will only zoom
	 * vertically. If you press alt+ctrl, it will only zoom horizontally. To
	 * change these default mappings, use
	 * {@link WCartesianChart#setWheelActions(Map wheelActions)
	 * setWheelActions()}.
	 * <p>
	 * When using touch, you can use a pinch gesture to zoom in/out. If the
	 * pinch gesture is vertical/horizontal, it will zoom only
	 * vertically/horizontally, otherwise it will zoom both axes equally.
	 * <p>
	 * The default value is <code>false</code>.
	 * <p>
	 * 
	 * @see WCartesianChart#isZoomEnabled()
	 * @see WCartesianChart#setWheelActions(Map wheelActions)
	 */
	public void setZoomEnabled(boolean zoomEnabled) {
		if (this.zoomEnabled_ != zoomEnabled) {
			this.zoomEnabled_ = zoomEnabled;
			this.updateJSConfig("zoom", this.zoomEnabled_);
		}
	}

	/**
	 * Enables zoom functionality.
	 * <p>
	 * Calls {@link #setZoomEnabled(boolean zoomEnabled) setZoomEnabled(true)}
	 */
	public final void setZoomEnabled() {
		setZoomEnabled(true);
	}

	/**
	 * Returns whether zoom is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setZoomEnabled(boolean zoomEnabled)
	 */
	public boolean isZoomEnabled() {
		return this.zoomEnabled_;
	}

	/**
	 * Enables pan functionality.
	 * <p>
	 * When using the mouse, you can click and drag to pan the chart (if zoomed
	 * in), or use the scrollwheel.
	 * <p>
	 * When using touch, you can drag to pan the chart. If the rubberband effect
	 * is enabled, this is intertial (it will keep scrolling after you let go)
	 * and there is an overscroll and bounce back effect on the sides.
	 * <p>
	 * The default value is <code>false</code>.
	 * <p>
	 * 
	 * @see WCartesianChart#isPanEnabled()
	 */
	public void setPanEnabled(boolean panEnabled) {
		if (this.panEnabled_ != panEnabled) {
			this.panEnabled_ = panEnabled;
			this.updateJSConfig("pan", this.panEnabled_);
		}
	}

	/**
	 * Enables pan functionality.
	 * <p>
	 * Calls {@link #setPanEnabled(boolean panEnabled) setPanEnabled(true)}
	 */
	public final void setPanEnabled() {
		setPanEnabled(true);
	}

	/**
	 * Returns whether pan is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setPanEnabled(boolean panEnabled)
	 */
	public boolean isPanEnabled() {
		return this.panEnabled_;
	}

	/**
	 * Enables the crosshair functionality.
	 * <p>
	 * When enabled, the crosshair will follow mouse movement, and show in the
	 * top right corner the coordinate (according to X axis and the first Y
	 * axis) corresponding to this position.
	 * <p>
	 * When using touch, the crosshair can be moved with a drag. If both panning
	 * and the crosshair are enabled, the crosshair will be moved when dragging
	 * close to the crosshair. Otherwise, the chart will pan.
	 */
	public void setCrosshairEnabled(boolean crosshair) {
		if (this.crosshairEnabled_ != crosshair) {
			this.crosshairEnabled_ = crosshair;
			this.updateJSConfig("crosshair", this.crosshairEnabled_);
		}
	}

	/**
	 * Enables the crosshair functionality.
	 * <p>
	 * Calls {@link #setCrosshairEnabled(boolean crosshair)
	 * setCrosshairEnabled(true)}
	 */
	public final void setCrosshairEnabled() {
		setCrosshairEnabled(true);
	}

	/**
	 * Returns whether the crosshair is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
	 */
	public boolean isCrosshairEnabled() {
		return this.crosshairEnabled_;
	}

	/**
	 * Sets the crosshair color.
	 * <p>
	 * The crosshair color is black by default.
	 * <p>
	 * 
	 * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
	 */
	public void setCrosshairColor(final WColor color) {
		if (!this.crosshairColor_.equals(color)) {
			this.crosshairColor_ = color;
			this.updateJSConfig("crosshairColor",
					jsStringLiteral(color.getCssText(true)));
		}
	}

	/**
	 * Returns the crosshair color.
	 * <p>
	 * 
	 * @see WCartesianChart#setCrosshairEnabled(boolean crosshair)
	 * @see WCartesianChart#setCrosshairColor(WColor color)
	 */
	public WColor getCrosshairColor() {
		return this.crosshairColor_;
	}

	/**
	 * Sets the Y axis to use for the crosshair.
	 * <p>
	 * Defaults to 0 (first Y axis)
	 */
	public void setCrosshairYAxis(int yAxis) {
		if (this.crosshairYAxis_ != yAxis) {
			this.crosshairYAxis_ = yAxis;
			this.updateJSConfig("crosshairAxis", yAxis);
		}
	}

	/**
	 * Returns the Y axis to use for the crosshair.
	 */
	public int getCrosshairYAxis() {
		return this.crosshairYAxis_;
	}

	/**
	 * Enables the follow curve functionality for a data series.
	 * <p>
	 * This enables follow curve functionality for the data series corresponding
	 * to the given column.
	 * <p>
	 * If the data series is of type LineSeries or CurveSeries, the crosshair
	 * can only be moved in the x direction. The y position of the crosshair
	 * will be determined by the value of the data series. The crosshair will
	 * snap to the nearest point that is defined in the data series.
	 * <p>
	 * When using the mouse, the x position will change on mouseover. When using
	 * touch, the x position can be moved with a drag. The follow curve
	 * functionality has priority over the crosshair functionality.
	 * <p>
	 * Use column index -1 or {@link WCartesianChart#disableFollowCurve()
	 * disableFollowCurve()} to disable the follow curve feature.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>The follow curve functionality requires that the X axis
	 * values of the data series are monotonically increasing or decreasing.</i>
	 * </p>
	 * 
	 * @deprecated Use {@link WCartesianChart#setFollowCurve(WDataSeries series)
	 *             setFollowCurve()} instead
	 */
	public void setFollowCurve(int followCurve) {
		if (followCurve == -1) {
			this.setFollowCurve((WDataSeries) null);
		} else {
			for (int i = 0; i < this.series_.size(); ++i) {
				if (this.series_.get(i).getModelColumn() == followCurve) {
					this.setFollowCurve(this.series_.get(i));
				}
			}
		}
	}

	/**
	 * Enabled the follow curve funtionality for a data series.
	 * <p>
	 * This enables follow curve functionality for the data series corresponding
	 * to the given column.
	 * <p>
	 * If the data series is of type LineSeries or CurveSeries, the crosshair
	 * can only be moved in the x direction. The y position of the crosshair
	 * will be determined by the value of the data series. The crosshair will
	 * snap to the nearest point that is defined in the data series.
	 * <p>
	 * When using the mouse, the x position will change on mouseover. When using
	 * touch, the x position can be moved with a drag. The follow curve
	 * functionality has priority over the crosshair functionality.
	 * <p>
	 * Set to null to disable the follow curve feature.
	 * <p>
	 * <p>
	 * <i><b>Note: </b>The follow curve functionality requires that the X axis
	 * values of the data series are monotonically increasing or decreasing.
	 * </i>
	 * </p>
	 */
	public void setFollowCurve(WDataSeries series) {
		if (this.followCurve_ != series) {
			this.followCurve_ = series;
			this.updateJSConfig("followCurve",
					series != null ? this.getSeriesIndexOf(series) : -1);
		}
	}

	/**
	 * Disable the follow curve functionality.
	 * <p>
	 * 
	 * @see WCartesianChart#setFollowCurve(int followCurve)
	 */
	public void disableFollowCurve() {
		this.setFollowCurve((WDataSeries) null);
	}

	/**
	 * Returns the curve that is to be followed.
	 * <p>
	 * If follow curve functionality is not enabled, returns -1.
	 * <p>
	 * 
	 * @see WCartesianChart#setFollowCurve(int followCurve)
	 */
	public WDataSeries getFollowCurve() {
		return this.followCurve_;
	}

	/**
	 * Enables/disables the inertial scrolling and rubberband effect.
	 * <p>
	 * 
	 * @see WCartesianChart#setPanEnabled(boolean panEnabled)
	 */
	public void setRubberBandEffectEnabled(boolean rubberBandEnabled) {
		if (this.rubberBandEnabled_ != rubberBandEnabled) {
			this.rubberBandEnabled_ = rubberBandEnabled;
			this.updateJSConfig("rubberBand", this.rubberBandEnabled_);
		}
	}

	/**
	 * Enables/disables the inertial scrolling and rubberband effect.
	 * <p>
	 * Calls {@link #setRubberBandEffectEnabled(boolean rubberBandEnabled)
	 * setRubberBandEffectEnabled(true)}
	 */
	public final void setRubberBandEffectEnabled() {
		setRubberBandEffectEnabled(true);
	}

	/**
	 * Checks whether the rubberband effect is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setRubberBandEffectEnabled(boolean
	 *      rubberBandEnabled)
	 */
	public boolean isRubberBandEffectEnabled() {
		return this.rubberBandEnabled_;
	}

	/**
	 * Sets the mapping of mouse wheel actions for interactive charts.
	 * <p>
	 * 
	 * @see WCartesianChart#getWheelActions()
	 */
	public void setWheelActions(
			Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions) {
		this.wheelActions_ = wheelActions;
		this.updateJSConfig("wheelActions",
				wheelActionsToJson(this.wheelActions_));
	}

	/**
	 * Returns the current mouse wheel actions for interactive charts.
	 * <p>
	 * 
	 * @see WCartesianChart#setWheelActions(Map wheelActions)
	 */
	public Map<EnumSet<KeyboardModifier>, InteractiveAction> getWheelActions() {
		return this.wheelActions_;
	}

	/**
	 * Enables or disables soft label clipping on all axes.
	 * <p>
	 * 
	 * @see WAxis#setSoftLabelClipping(boolean enabled)
	 */
	public void setSoftLabelClipping(boolean enabled) {
		this.xAxis_.axis.setSoftLabelClipping(enabled);
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).axis.setSoftLabelClipping(enabled);
		}
	}

	/**
	 * Sets whether series selection is enabled.
	 * <p>
	 * If series selection is enabled, series can be selected with a mouse click
	 * or long press. If the selected series is a LineSeries or CurveSeries, it
	 * can be manipulated if
	 * {@link WCartesianChart#setCurveManipulationEnabled(boolean enabled) curve
	 * manipulation} is enabled. The series that are not selected, will be shown
	 * in a lighter color.
	 */
	public void setSeriesSelectionEnabled(boolean enabled) {
		if (this.seriesSelectionEnabled_ != enabled) {
			this.seriesSelectionEnabled_ = enabled;
			this.updateJSConfig("seriesSelection", this.seriesSelectionEnabled_);
		}
	}

	/**
	 * Sets whether series selection is enabled.
	 * <p>
	 * Calls {@link #setSeriesSelectionEnabled(boolean enabled)
	 * setSeriesSelectionEnabled(true)}
	 */
	public final void setSeriesSelectionEnabled() {
		setSeriesSelectionEnabled(true);
	}

	/**
	 * Returns whether series selection is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
	 */
	public boolean isSeriesSelectionEnabled() {
		return this.seriesSelectionEnabled_;
	}

	/**
	 * A signal that notifies the selection of a new curve.
	 * <p>
	 * This signal is emitted if a series is selected using a mouse click or
	 * long press. The first argument is the selected series. The second
	 * argument is the point that was selected, in model coordinates.
	 * <p>
	 * 
	 * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
	 */
	public Signal2<WDataSeries, WPointF> seriesSelected() {
		return this.seriesSelected_;
	}

	/**
	 * Sets the series that is currently selected.
	 * <p>
	 * The series with the given model column will be selected. The other series
	 * will be shown in a lighter color. The series that is currently selected
	 * is the one that can be manipulated if
	 * {@link WCartesianChart#setCurveManipulationEnabled(boolean enabled) curve
	 * manipulation} is enabled, and it is a LineSeries or CurveSeries.
	 * <p>
	 * The selected series can be changed using a long touch or mouse click.
	 * <p>
	 * If the argument provided is null or
	 * {@link WCartesianChart#setSeriesSelectionEnabled(boolean enabled) series
	 * selection} is not enabled, no series will be selected.
	 * <p>
	 * 
	 * @see WCartesianChart#setCurveManipulationEnabled(boolean enabled)
	 * @see WCartesianChart#setSeriesSelectionEnabled(boolean enabled)
	 */
	public void setSelectedSeries(WDataSeries series) {
		if (this.selectedSeries_ != series) {
			this.selectedSeries_ = series;
			this.update();
		}
	}

	/**
	 * Get the currently selected curve.
	 * <p>
	 * -1 means that no curve is currently selected.
	 * <p>
	 * 
	 * @see WCartesianChart#setSelectedSeries(WDataSeries series)
	 */
	public WDataSeries getSelectedSeries() {
		return this.selectedSeries_;
	}

	/**
	 * Enable curve manipulation.
	 * <p>
	 * If curve manipulation is enabled, the
	 * {@link WDataSeries#setScale(double scale) scale} and
	 * {@link WDataSeries#setOffset(double offset) offset} of the
	 * {@link WCartesianChart#getSelectedSeries() selected curve} can be
	 * manipulated interactively using drag, scroll, and pinch.
	 * <p>
	 * 
	 * @see WDataSeries#setOffset(double offset)
	 * @see WDataSeries#setScale(double scale)
	 */
	public void setCurveManipulationEnabled(boolean enabled) {
		if (this.curveManipulationEnabled_ != enabled) {
			this.curveManipulationEnabled_ = enabled;
			this.updateJSConfig("curveManipulation",
					this.curveManipulationEnabled_);
		}
	}

	/**
	 * Enable curve manipulation.
	 * <p>
	 * Calls {@link #setCurveManipulationEnabled(boolean enabled)
	 * setCurveManipulationEnabled(true)}
	 */
	public final void setCurveManipulationEnabled() {
		setCurveManipulationEnabled(true);
	}

	/**
	 * Returns whether curve manipulation is enabled.
	 * <p>
	 * 
	 * @see WCartesianChart#setCurveManipulationEnabled(boolean enabled)
	 */
	public boolean isCurveManipulationEnabled() {
		return this.curveManipulationEnabled_;
	}

	public void iterateSeries(SeriesIterator iterator, WPainter painter,
			boolean reverseStacked) {
		double groupWidth = 0.0;
		int numBarGroups;
		int currentBarGroup;
		int rowCount = this.getModel() != null ? this.getModel().getRowCount()
				: 0;
		List<Double> posStackedValuesInit = new ArrayList<Double>();
		List<Double> minStackedValuesInit = new ArrayList<Double>();
		{
			int insertPos = 0;
			for (int ii = 0; ii < (rowCount); ++ii)
				posStackedValuesInit.add(insertPos + ii, 0.0);
		}
		;
		{
			int insertPos = 0;
			for (int ii = 0; ii < (rowCount); ++ii)
				minStackedValuesInit.add(insertPos + ii, 0.0);
		}
		;
		final boolean scatterPlot = this.type_ == ChartType.ScatterPlot;
		if (scatterPlot) {
			numBarGroups = 1;
			currentBarGroup = 0;
		} else {
			numBarGroups = this.getCalcNumBarGroups();
			currentBarGroup = 0;
		}
		boolean containsBars = false;
		for (int g = 0; g < this.series_.size(); ++g) {
			if (this.series_.get(g).isHidden()
					&& !(this.axisSliderWidgetForSeries(this.series_.get(g)) && (((iterator) instanceof SeriesRenderIterator ? (SeriesRenderIterator) (iterator)
							: null) != null || ((iterator) instanceof ExtremesIterator ? (ExtremesIterator) (iterator)
							: null) != null))) {
				continue;
			}
			groupWidth = this.series_.get(g).getBarWidth()
					* (this.map(2, 0).getX() - this.map(1, 0).getX());
			if (containsBars) {
				++currentBarGroup;
			}
			containsBars = false;
			int startSeries;
			int endSeries;
			if (scatterPlot) {
				startSeries = endSeries = g;
			} else {
				if (this.series_.get(g).getModel() == this.getModel()) {
					for (int i = 0; i < rowCount; ++i) {
						posStackedValuesInit.set(i,
								minStackedValuesInit.set(i, 0.0));
					}
					if (reverseStacked) {
						endSeries = g;
						int a = this.series_.get(g).getYAxis();
						for (;;) {
							if (g < this.series_.size()
									&& ((int) g == endSeries || this.series_
											.get(g).isStacked())
									&& this.series_.get(g).getYAxis() == a) {
								if (this.series_.get(g).getType() == SeriesType.BarSeries) {
									containsBars = true;
								}
								for (int row = 0; row < rowCount; ++row) {
									double y = StringUtils.asNumber(this
											.getModel().getData(
													row,
													this.series_.get(g)
															.getModelColumn()));
									if (!Double.isNaN(y)) {
										if (y > 0) {
											posStackedValuesInit.set(
													row,
													posStackedValuesInit
															.get(row) + y);
										} else {
											minStackedValuesInit.set(
													row,
													minStackedValuesInit
															.get(row) + y);
										}
									}
								}
								++g;
							} else {
								break;
							}
						}
						--g;
						startSeries = g;
					} else {
						startSeries = g;
						int a = this.series_.get(g).getYAxis();
						if (this.series_.get(g).getType() == SeriesType.BarSeries) {
							containsBars = true;
						}
						++g;
						for (;;) {
							if (g < this.series_.size()
									&& this.series_.get(g).isStacked()
									&& this.series_.get(g).getYAxis() == a) {
								if (this.series_.get(g).getType() == SeriesType.BarSeries) {
									containsBars = true;
								}
								++g;
							} else {
								break;
							}
						}
						--g;
						endSeries = g;
					}
				} else {
					throw new WException(
							"Configuring different models for WDataSeries are unsupported for category charts!");
				}
			}
			int i = startSeries;
			for (;;) {
				boolean doSeries = iterator.startSeries(this.series_.get(i),
						groupWidth, numBarGroups, currentBarGroup);
				List<Double> posStackedValues = new ArrayList<Double>();
				List<Double> minStackedValues = new ArrayList<Double>();
				if (doSeries || !scatterPlot && i != endSeries) {
					for (int currentXSegment = 0; currentXSegment < this
							.getAxis(Axis.XAxis).getSegmentCount(); ++currentXSegment) {
						for (int currentYSegment = 0; currentYSegment < this
								.yAxis(this.series_.get(i).getYAxis())
								.getSegmentCount(); ++currentYSegment) {
							posStackedValues.clear();
							posStackedValues.addAll(posStackedValuesInit);
							minStackedValues.clear();
							minStackedValues.addAll(minStackedValuesInit);
							if (painter != null) {
								WRectF csa = this.chartSegmentArea(this
										.yAxis(this.series_.get(i).getYAxis()),
										currentXSegment, currentYSegment);
								iterator.startSegment(currentXSegment,
										currentYSegment, csa);
								painter.save();
								if (!this.isInteractive()) {
									WPainterPath clipPath = new WPainterPath();
									clipPath.addRect(this.hv(csa));
									painter.setClipPath(clipPath);
									painter.setClipping(true);
								}
							} else {
								iterator.startSegment(currentXSegment,
										currentYSegment, null);
							}
							for (int row = 0; row < (this.series_.get(i)
									.getModel() != null ? this.series_.get(i)
									.getModel().getRowCount() : 0); ++row) {
								int[] xIndex = { -1, -1 };
								int[] yIndex = { -1, -1 };
								double x;
								if (scatterPlot) {
									int c = this.series_.get(i).XSeriesColumn();
									if (c == -1) {
										c = this.XSeriesColumn();
									}
									if (c != -1) {
										xIndex[0] = row;
										xIndex[1] = c;
										x = this.series_.get(i).getModel()
												.getData(xIndex[0], xIndex[1]);
									} else {
										x = row;
									}
								} else {
									x = row;
								}
								yIndex[0] = row;
								yIndex[1] = this.series_.get(i)
										.getModelColumn();
								double y = this.series_.get(i).getModel()
										.getData(yIndex[0], yIndex[1]);
								if (scatterPlot) {
									iterator.newValue(this.series_.get(i), x,
											y, 0, xIndex[0], xIndex[1],
											yIndex[0], yIndex[1]);
								} else {
									double prevStack = 0;
									double nextStack = 0;
									boolean hasValue = !Double.isNaN(y);
									if (hasValue) {
										if (y > 0) {
											prevStack = nextStack = posStackedValues
													.get(row);
										} else {
											prevStack = nextStack = minStackedValues
													.get(row);
										}
										if (reverseStacked) {
											nextStack -= y;
										} else {
											nextStack += y;
										}
										if (y > 0) {
											posStackedValues
													.set(row, nextStack);
										} else {
											minStackedValues
													.set(row, nextStack);
										}
									}
									if (doSeries) {
										if (reverseStacked) {
											iterator.newValue(
													this.series_.get(i), x,
													hasValue ? prevStack : y,
													nextStack, xIndex[0],
													xIndex[1], yIndex[0],
													yIndex[1]);
										} else {
											iterator.newValue(
													this.series_.get(i), x,
													hasValue ? nextStack : y,
													prevStack, xIndex[0],
													xIndex[1], yIndex[0],
													yIndex[1]);
										}
									}
								}
							}
							iterator.endSegment();
							if (painter != null) {
								painter.restore();
							}
						}
					}
					posStackedValuesInit.clear();
					posStackedValuesInit.addAll(posStackedValues);
					minStackedValuesInit.clear();
					minStackedValuesInit.addAll(minStackedValues);
				}
				if (doSeries) {
					iterator.endSeries();
				}
				if (i == endSeries) {
					break;
				} else {
					if (endSeries < startSeries) {
						--i;
					} else {
						++i;
					}
				}
			}
		}
	}

	public final void iterateSeries(SeriesIterator iterator, WPainter painter) {
		iterateSeries(iterator, painter, false);
	}

	void addAxisSliderWidget(WAxisSliderWidget slider) {
		this.axisSliderWidgets_.add(slider);
		StringBuilder ss = new StringBuilder();
		ss.append('[');
		for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
			if (i != 0) {
				ss.append(',');
			}
			ss.append('"').append(this.axisSliderWidgets_.get(i).getId())
					.append('"');
		}
		ss.append(']');
		this.updateJSConfig("sliders", ss.toString());
	}

	void removeAxisSliderWidget(WAxisSliderWidget slider) {
		for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
			if (slider == this.axisSliderWidgets_.get(i)) {
				this.axisSliderWidgets_.remove(0 + i);
				StringBuilder ss = new StringBuilder();
				ss.append('[');
				for (int j = 0; j < this.axisSliderWidgets_.size(); ++j) {
					if (j != 0) {
						ss.append(',');
					}
					ss.append('"')
							.append(this.axisSliderWidgets_.get(j).getId())
							.append('"');
				}
				ss.append(']');
				this.updateJSConfig("sliders", ss.toString());
				return;
			}
		}
	}

	static class AxisLocation {
		private static Logger logger = LoggerFactory
				.getLogger(AxisLocation.class);

		public AxisLocation() {
			this.minOffset = 0;
			this.maxOffset = 0;
			this.loc = AxisValue.MinimumValue;
		}

		public int minOffset;
		public int maxOffset;
		public AxisValue loc;
	}

	static class PenAssignment {
		private static Logger logger = LoggerFactory
				.getLogger(PenAssignment.class);

		public WJavaScriptHandle<WPen> pen;
		public WJavaScriptHandle<WPen> textPen;
		public WJavaScriptHandle<WPen> gridPen;

		public PenAssignment(final WJavaScriptHandle<WPen> pen,
				final WJavaScriptHandle<WPen> textPen,
				final WJavaScriptHandle<WPen> gridPen) {
			this.pen = pen;
			this.textPen = textPen;
			this.gridPen = gridPen;
		}
	}

	static class AxisStruct {
		private static Logger logger = LoggerFactory
				.getLogger(AxisStruct.class);

		public AxisStruct() {
			this.axis = new WAxis();
			this.calculatedWidth = 0;
			this.location = new WCartesianChart.AxisLocation();
			this.transform = new WTransform();
			this.transformHandle = null;
			this.transformChanged = null;
			this.pens = new ArrayList<WCartesianChart.PenAssignment>();
		}

		public AxisStruct(WAxis ax) {
			this.axis = ax;
			this.calculatedWidth = 0;
			this.location = new WCartesianChart.AxisLocation();
			this.transform = new WTransform();
			this.transformHandle = null;
			this.transformChanged = null;
			this.pens = new ArrayList<WCartesianChart.PenAssignment>();
		}

		public WAxis axis;
		public int calculatedWidth;
		public WCartesianChart.AxisLocation location;
		public WTransform transform;
		public WJavaScriptHandle<WTransform> transformHandle;
		public JSignal transformChanged;
		public List<WCartesianChart.PenAssignment> pens;
	}

	private WChart2DImplementation interface_;
	private Orientation orientation_;
	private int XSeriesColumn_;
	private ChartType type_;
	private List<WDataSeries> series_;
	WCartesianChart.AxisStruct xAxis_;
	private List<WCartesianChart.AxisStruct> yAxes_;
	private double barMargin_;
	private WLegend legend_;
	private int axisPadding_;
	private WPen borderPen_;
	WPen textPen_;
	private int width_;
	private int height_;
	WRectF chartArea_;
	boolean hasDeferredToolTips_;
	private boolean jsDefined_;
	private boolean zoomEnabled_;
	private boolean panEnabled_;
	private boolean rubberBandEnabled_;
	private boolean crosshairEnabled_;
	private WColor crosshairColor_;
	private int crosshairYAxis_;
	private boolean seriesSelectionEnabled_;
	private WDataSeries selectedSeries_;
	private WDataSeries followCurve_;
	private boolean curveManipulationEnabled_;
	boolean cObjCreated_;
	private Signal2<WDataSeries, WPointF> seriesSelected_;
	private JSignal2<Double, Double> jsSeriesSelected_;
	Map<WDataSeries, WJavaScriptHandle<WPainterPath>> curvePaths_;
	private List<WJavaScriptHandle<WPainterPath>> freePainterPaths_;
	Map<WDataSeries, WJavaScriptHandle<WTransform>> curveTransforms_;
	private List<WJavaScriptHandle<WTransform>> freeTransforms_;
	private List<WJavaScriptHandle<WPen>> freePens_;
	private List<CurveLabel> curveLabels_;
	private List<WAxisSliderWidget> axisSliderWidgets_;
	private Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions_;
	private JSignal2<Double, Double> loadTooltip_;

	static class BarTooltip {
		private static Logger logger = LoggerFactory
				.getLogger(BarTooltip.class);

		public BarTooltip(final WDataSeries series, int xRow, int xColumn,
				int yRow, int yColumn) {
			this.series = series;
			this.xRow = xRow;
			this.xColumn = xColumn;
			this.yRow = yRow;
			this.yColumn = yColumn;
		}

		public double[] xs = new double[4];
		public double[] ys = new double[4];
		public WDataSeries series;
		public int xRow;
		public int xColumn;
		public int yRow;
		public int yColumn;
	}

	List<WCartesianChart.BarTooltip> barTooltips_;

	private void init() {
		this.setPalette(new WStandardPalette(WStandardPalette.Flavour.Muted));
		this.yAxes_.add(new WCartesianChart.AxisStruct());
		this.yAxes_.add(new WCartesianChart.AxisStruct());
		this.yAxes_.get(this.yAxes_.size() - 1).axis
				.setLocation(AxisValue.MaximumValue);
		this.getAxis(Axis.XAxis).init(this.interface_, Axis.XAxis);
		this.getAxis(Axis.YAxis).init(this.interface_, Axis.YAxis);
		this.getAxis(Axis.Y2Axis).init(this.interface_, Axis.Y2Axis);
		this.getAxis(Axis.XAxis).setPadding(this.axisPadding_);
		this.getAxis(Axis.YAxis).setPadding(this.axisPadding_);
		this.getAxis(Axis.Y2Axis).setPadding(this.axisPadding_);
		this.getAxis(Axis.XAxis).setSoftLabelClipping(true);
		this.getAxis(Axis.YAxis).setSoftLabelClipping(true);
		this.getAxis(Axis.Y2Axis).setSoftLabelClipping(true);
		this.setPlotAreaPadding(40, EnumSet.of(Side.Left, Side.Right));
		this.setPlotAreaPadding(30, EnumSet.of(Side.Top, Side.Bottom));
		this.xAxis_.transformHandle = this.createJSTransform();
		this.xAxis_.transformChanged = new JSignal(this, "xTransformChanged");
		for (int i = 0; i < 2; ++i) {
			this.yAxes_.get(i).transformHandle = this.createJSTransform();
			this.yAxes_.get(i).transformChanged = new JSignal(this,
					"yTransformChanged" + String.valueOf(i));
		}
		if (WApplication.getInstance() != null
				&& WApplication.getInstance().getEnvironment().hasAjax()) {
			this.mouseWentDown().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseDown(o, e);}}");
			this.mouseWentUp().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseUp(o, e);}}");
			this.mouseDragged().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseDrag(o, e);}}");
			this.mouseMoved().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseMove(o, e);}}");
			this.mouseWheel().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseWheel(o, e);}}");
			this.mouseWentOut().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.mouseOut(o, e);}}");
			this.touchStarted().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.touchStart(o, e);}}");
			this.touchEnded().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.touchEnd(o, e);}}");
			this.touchMoved().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.touchMoved(o, e);}}");
			this.clicked().addListener(
					"function(o, e){var o=" + this.getCObjJsRef()
							+ ";if(o){o.clicked(o, e);}}");
			this.jsSeriesSelected_.addListener(this,
					new Signal2.Listener<Double, Double>() {
						public void trigger(Double e1, Double e2) {
							WCartesianChart.this.jsSeriesSelected(e1, e2);
						}
					});
			this.loadTooltip_.addListener(this,
					new Signal2.Listener<Double, Double>() {
						public void trigger(Double e1, Double e2) {
							WCartesianChart.this.loadTooltip(e1, e2);
						}
					});
		}
		this.wheelActions_.put(EnumSet.of(KeyboardModifier.NoModifier),
				InteractiveAction.PanMatching);
		this.wheelActions_.put(EnumSet.of(KeyboardModifier.AltModifier,
				KeyboardModifier.ControlModifier), InteractiveAction.ZoomX);
		this.wheelActions_.put(EnumSet.of(KeyboardModifier.ControlModifier,
				KeyboardModifier.ShiftModifier), InteractiveAction.ZoomY);
		this.wheelActions_.put(EnumSet.of(KeyboardModifier.ControlModifier),
				InteractiveAction.ZoomXY);
		this.wheelActions_.put(EnumSet.of(KeyboardModifier.AltModifier,
				KeyboardModifier.ControlModifier,
				KeyboardModifier.ShiftModifier), InteractiveAction.ZoomXY);
	}

	private static String wheelActionsToJson(
			Map<EnumSet<KeyboardModifier>, InteractiveAction> wheelActions) {
		StringBuilder ss = new StringBuilder();
		ss.append('{');
		boolean first = true;
		for (Iterator<Map.Entry<EnumSet<KeyboardModifier>, InteractiveAction>> it_it = wheelActions
				.entrySet().iterator(); it_it.hasNext();) {
			Map.Entry<EnumSet<KeyboardModifier>, InteractiveAction> it = it_it
					.next();
			if (first) {
				first = false;
			} else {
				ss.append(',');
			}
			ss.append(EnumUtils.valueOf(it.getKey())).append(':')
					.append((int) it.getValue().getValue());
		}
		ss.append('}');
		return ss.toString();
	}

	static WColor lightenColor(final WColor in) {
		double[] hsl = new double[3];
		in.toHSL(hsl);
		double h = hsl[0];
		double s = hsl[1];
		double l = hsl[2];
		return WColor.fromHSL(h, Math.max(s - 0.2, 0.0),
				Math.min(l + 0.2, 1.0), in.getAlpha());
	}

	protected void getDomChanges(final List<DomElement> result, WApplication app) {
		super.getDomChanges(result, app);
		for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
			this.axisSliderWidgets_.get(i).update();
		}
	}

	protected void modelChanged() {
		this.XSeriesColumn_ = -1;
		while (this.axisSliderWidgets_.size() > 0) {
			this.axisSliderWidgets_.get(this.axisSliderWidgets_.size() - 1)
					.setSeries((WDataSeries) null);
		}
		this.freeAllJSPaths();
		this.freeAllJSTransforms();
		for (int i = 0; i < this.series_.size(); ++i) {
			;
		}
		this.series_.clear();
		this.update();
	}

	protected void modelReset() {
		this.update();
	}

	/**
	 * Paints the widget.
	 * <p>
	 * This calls
	 * {@link WCartesianChart#render(WPainter painter, WRectF rectangle)
	 * render()} to paint on the paint device.
	 */
	protected void paintEvent(WPaintDevice paintDevice) {
		this.hasDeferredToolTips_ = false;
		WPainter painter = new WPainter(paintDevice);
		painter.setRenderHint(WPainter.RenderHint.Antialiasing);
		this.paint(painter);
		if (this.hasDeferredToolTips_ && !this.jsDefined_) {
			this.defineJavaScript();
		}
		if (this.isInteractive() || this.hasDeferredToolTips_) {
			this.setZoomAndPan();
			List<WRectF> modelAreas = new ArrayList<WRectF>();
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				double modelBottom = this.yAxis(i).mapFromDevice(0);
				double modelTop = this.yAxis(i).mapFromDevice(
						this.chartArea_.getHeight());
				double modelLeft = this.getAxis(Axis.XAxis).mapFromDevice(0);
				double modelRight = this.getAxis(Axis.XAxis).mapFromDevice(
						this.chartArea_.getWidth());
				WRectF modelArea = new WRectF(modelLeft, modelBottom,
						modelRight - modelLeft, modelTop - modelBottom);
				modelAreas.add(modelArea);
			}
			WRectF insideArea = this.getInsideChartArea();
			int coordPaddingX = 5;
			int coordPaddingY = 5;
			if (this.getOrientation() == Orientation.Vertical) {
				if (this.getAxis(Axis.XAxis).isVisible()
						&& this.getAxis(Axis.XAxis).getTickDirection() == TickDirection.Inwards
						&& (this.getAxis(Axis.XAxis).getLocation() == AxisValue.MaximumValue || this
								.getAxis(Axis.XAxis).getLocation() == AxisValue.BothSides)) {
					coordPaddingY = 25;
				}
				if (this.getAxis(Axis.Y1Axis).isVisible()
						&& this.getAxis(Axis.Y1Axis).getTickDirection() == TickDirection.Inwards
						&& (this.getAxis(Axis.Y1Axis).getLocation() == AxisValue.MaximumValue || this
								.getAxis(Axis.Y1Axis).getLocation() == AxisValue.BothSides)
						|| this.getAxis(Axis.Y2Axis).isVisible()
						&& this.getAxis(Axis.Y2Axis).getTickDirection() == TickDirection.Inwards) {
					coordPaddingX = 40;
				}
			} else {
				if (this.getAxis(Axis.XAxis).isVisible()
						&& this.getAxis(Axis.XAxis).getTickDirection() == TickDirection.Inwards
						&& (this.getAxis(Axis.XAxis).getLocation() == AxisValue.MaximumValue || this
								.getAxis(Axis.XAxis).getLocation() == AxisValue.BothSides)) {
					coordPaddingX = 40;
				}
				if (this.getAxis(Axis.Y1Axis).isVisible()
						&& this.getAxis(Axis.Y1Axis).getTickDirection() == TickDirection.Inwards
						&& (this.getAxis(Axis.Y1Axis).getLocation() == AxisValue.MinimumValue || this
								.getAxis(Axis.Y1Axis).getLocation() == AxisValue.BothSides)) {
					coordPaddingY = 25;
				}
			}
			if (this.getAxis(Axis.XAxis).zoomRangeChanged().isConnected()
					&& !this.xAxis_.transformChanged.isConnected()) {
				this.xAxis_.transformChanged.addListener(this,
						new Signal.Listener() {
							public void trigger() {
								WCartesianChart.this.xTransformChanged();
							}
						});
			}
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (this.yAxis(i).zoomRangeChanged().isConnected()
						&& !this.yAxes_.get(i).transformChanged.isConnected()) {
					final int axis = i;
					this.yAxes_.get(i).transformChanged.addListener(this,
							new Signal.Listener() {
								public void trigger() {
									WCartesianChart.this
											.yTransformChanged(axis);
								}
							});
				}
			}
			char[] buf = new char[30];
			WApplication app = WApplication.getInstance();
			StringBuilder ss = new StringBuilder();
			int selectedCurve = this.selectedSeries_ != null ? this
					.getSeriesIndexOf(this.selectedSeries_) : -1;
			int followCurve = this.followCurve_ != null ? this
					.getSeriesIndexOf(this.followCurve_) : -1;
			ss.append("new Wt3_3_10.WCartesianChart(")
					.append(app.getJavaScriptClass())
					.append(",")
					.append(this.getJsRef())
					.append(",")
					.append(this.getObjJsRef())
					.append(",{curveManipulation:")
					.append(StringUtils
							.asString(this.curveManipulationEnabled_)
							.toString())
					.append(",seriesSelection:")
					.append(StringUtils.asString(this.seriesSelectionEnabled_)
							.toString())
					.append(",selectedCurve:")
					.append(selectedCurve)
					.append(",isHorizontal:")
					.append(StringUtils.asString(
							this.getOrientation() == Orientation.Horizontal)
							.toString())
					.append(",zoom:")
					.append(StringUtils.asString(this.zoomEnabled_).toString())
					.append(",pan:")
					.append(StringUtils.asString(this.panEnabled_).toString())
					.append(",crosshair:")
					.append(StringUtils.asString(this.crosshairEnabled_)
							.toString())
					.append(",crosshairAxis:")
					.append(this.crosshairYAxis_)
					.append(",crosshairColor:")
					.append(jsStringLiteral(this.crosshairColor_
							.getCssText(true))).append(",followCurve:")
					.append(followCurve).append(",xTransform:")
					.append(this.xAxis_.transformHandle.getJsRef())
					.append(",yTransforms:[");
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append(this.yAxes_.get(i).transformHandle.getJsRef());
			}
			ss.append("],area:").append(this.hv(this.chartArea_).getJsRef())
					.append(",insideArea:")
					.append(this.hv(insideArea).getJsRef())
					.append(",modelAreas:[");
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append(modelAreas.get(i).getJsRef());
			}
			ss.append("],");
			ss.append("hasToolTips:")
					.append(StringUtils.asString(this.hasDeferredToolTips_)
							.toString())
					.append(",notifyTransform:{x:")
					.append(StringUtils.asString(
							this.getAxis(Axis.XAxis).zoomRangeChanged()
									.isConnected()).toString()).append(",y:[");
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append(StringUtils.asString(
						this.yAxis(i).zoomRangeChanged().isConnected())
						.toString());
			}
			ss.append("]},ToolTipInnerStyle:")
					.append(jsStringLiteral(app.getTheme().utilityCssClass(
							UtilityCssClassRole.ToolTipInner)))
					.append(",ToolTipOuterStyle:")
					.append(jsStringLiteral(app.getTheme().utilityCssClass(
							UtilityCssClassRole.ToolTipOuter))).append(",");
			this.updateJSPens(ss);
			ss.append("series:{");
			{
				boolean firstCurvePath = true;
				for (int i = 0; i < this.series_.size(); ++i) {
					if (this.curvePaths_.get(this.series_.get(i)) != null) {
						if (firstCurvePath) {
							firstCurvePath = false;
						} else {
							ss.append(",");
						}
						ss.append((int) i).append(":{");
						ss.append("curve:")
								.append(this.curvePaths_.get(
										this.series_.get(i)).getJsRef())
								.append(",");
						ss.append("transform:")
								.append(this.curveTransforms_.get(
										this.series_.get(i)).getJsRef())
								.append(",");
						ss.append("axis:").append(
								this.series_.get(i).getYAxis());
						ss.append("}");
					}
				}
			}
			ss.append("},");
			ss.append("maxZoom:{x:")
					.append(MathUtils.roundJs(this.getAxis(Axis.XAxis)
							.getMaxZoom(), 3)).append(",y:[");
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append(MathUtils.roundJs(this.yAxis(i).getMaxZoom(), 3));
			}
			ss.append("]},");
			ss.append("rubberBand:").append(this.rubberBandEnabled_)
					.append(',');
			ss.append("sliders:[");
			for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append('"').append(this.axisSliderWidgets_.get(i).getId())
						.append('"');
			}
			ss.append("],");
			ss.append("wheelActions:")
					.append(wheelActionsToJson(this.wheelActions_)).append(",");
			ss.append("coordinateOverlayPadding:[").append(coordPaddingX)
					.append(",");
			ss.append(coordPaddingY).append("],");
			ss.append("yAxes:[");
			for (int i = 0; i < this.getYAxisCount(); ++i) {
				if (i != 0) {
					ss.append(',');
				}
				ss.append('{');
				ss.append("width:")
						.append(MathUtils.roundJs(
								this.yAxes_.get(i).calculatedWidth, 3))
						.append(',');
				ss.append("side:'");
				switch (this.yAxes_.get(i).location.loc) {
				case MinimumValue:
					ss.append("min");
					break;
				case MaximumValue:
					ss.append("max");
					break;
				case ZeroValue:
					ss.append("zero");
					break;
				case BothSides:
					ss.append("both");
					break;
				}
				ss.append("',");
				ss.append("minOffset:")
						.append(this.yAxes_.get(i).location.minOffset)
						.append(',');
				ss.append("maxOffset:").append(
						this.yAxes_.get(i).location.maxOffset);
				ss.append('}');
			}
			ss.append("]});");
			this.doJavaScript(ss.toString());
			this.cObjCreated_ = true;
		}
	}

	/**
	 * Renders the chart.
	 * <p>
	 * Renders the chart within the given rectangle. To accomodate both
	 * rendering of horizontal and vertically oriented charts, all rendering
	 * logic assumes horizontal. This &quot;chart coordinates&quot; space is
	 * transformed to painter coordinates using
	 * {@link WCartesianChart#hv(double x, double y) hv()}.
	 */
	protected void render(final WPainter painter, final WRectF rectangle) {
		painter.save();
		painter.translate(rectangle.getTopLeft());
		if (this.initLayout(rectangle, painter.getDevice())) {
			this.renderBackground(painter);
			this.renderGrid(painter, this.getAxis(Axis.XAxis));
			for (int i = 0; i < this.yAxes_.size(); ++i) {
				this.renderGrid(painter, this.yAxis(i));
			}
			this.renderAxes(painter, EnumSet.of(AxisProperty.Line));
			this.renderSeries(painter);
			this.renderAxes(painter, EnumSet.of(AxisProperty.Labels));
			this.renderBorder(painter);
			this.renderCurveLabels(painter);
			this.renderLegend(painter);
			this.renderOther(painter);
		}
		painter.restore();
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * The result needs further transformation using
	 * {@link WCartesianChart#hv(double x, double y) hv()} to painter
	 * coordinates.
	 */
	protected WPointF map(double xValue, double yValue, Axis yAxis,
			int currentXSegment, int currentYSegment) {
		return this.map(xValue, yValue, yAxis == Axis.Y1Axis ? 0 : 1,
				currentXSegment, currentYSegment);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * Returns
	 * {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int currentYSegment)
	 * map(xValue, yValue, Axis.OrdinateAxis, 0, 0)}
	 */
	protected final WPointF map(double xValue, double yValue) {
		return map(xValue, yValue, Axis.OrdinateAxis, 0, 0);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * Returns
	 * {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int currentYSegment)
	 * map(xValue, yValue, yAxis, 0, 0)}
	 */
	protected final WPointF map(double xValue, double yValue, Axis yAxis) {
		return map(xValue, yValue, yAxis, 0, 0);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * Returns
	 * {@link #map(double xValue, double yValue, Axis yAxis, int currentXSegment, int currentYSegment)
	 * map(xValue, yValue, yAxis, currentXSegment, 0)}
	 */
	protected final WPointF map(double xValue, double yValue, Axis yAxis,
			int currentXSegment) {
		return map(xValue, yValue, yAxis, currentXSegment, 0);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * The result needs further transformation using
	 * {@link WCartesianChart#hv(double x, double y) hv()} to painter
	 * coordinates.
	 */
	protected WPointF map(double xValue, double yValue, int yAxis,
			int currentXSegment, int currentYSegment) {
		final WAxis xAx = this.getAxis(Axis.XAxis);
		final WAxis yAx = this.yAxis(yAxis);
		double x = this.chartArea_.getLeft()
				+ xAx.mapToDevice(xValue, currentXSegment);
		double y = this.chartArea_.getBottom()
				- yAx.mapToDevice(yValue, currentYSegment);
		return new WPointF(x, y);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * Returns
	 * {@link #map(double xValue, double yValue, int yAxis, int currentXSegment, int currentYSegment)
	 * map(xValue, yValue, yAxis, 0, 0)}
	 */
	protected final WPointF map(double xValue, double yValue, int yAxis) {
		return map(xValue, yValue, yAxis, 0, 0);
	}

	/**
	 * Map (x, y) value pair to chart coordinates coordinates.
	 * <p>
	 * Returns
	 * {@link #map(double xValue, double yValue, int yAxis, int currentXSegment, int currentYSegment)
	 * map(xValue, yValue, yAxis, currentXSegment, 0)}
	 */
	protected final WPointF map(double xValue, double yValue, int yAxis,
			int currentXSegment) {
		return map(xValue, yValue, yAxis, currentXSegment, 0);
	}

	/**
	 * Utility function for rendering text.
	 * <p>
	 * This method renders text on the chart position <i>pos</i>, with a
	 * particular alignment <i>flags</i>. These are both specified in chart
	 * coordinates. The position is converted to painter coordinates using
	 * {@link WCartesianChart#hv(double x, double y) hv()}, and the alignment
	 * flags are changed accordingly. The rotation, indicated by <i>angle</i> is
	 * specified in painter coordinates and thus an angle of 0 always indicates
	 * horizontal text, regardless of the chart orientation.
	 */
	protected void renderLabel(final WPainter painter, final CharSequence text,
			final WPointF p, EnumSet<AlignmentFlag> flags, double angle,
			int margin) {
		AlignmentFlag horizontalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignHorizontalMask));
		AlignmentFlag verticalAlign = EnumUtils.enumFromSet(EnumUtils.mask(
				flags, AlignmentFlag.AlignVerticalMask));
		AlignmentFlag rHorizontalAlign = horizontalAlign;
		AlignmentFlag rVerticalAlign = verticalAlign;
		double width = 1000;
		double height = 20;
		WPointF pos = this.hv(p);
		if (this.getOrientation() == Orientation.Horizontal) {
			switch (horizontalAlign) {
			case AlignLeft:
				rVerticalAlign = AlignmentFlag.AlignTop;
				break;
			case AlignCenter:
				rVerticalAlign = AlignmentFlag.AlignMiddle;
				break;
			case AlignRight:
				rVerticalAlign = AlignmentFlag.AlignBottom;
				break;
			default:
				break;
			}
			switch (verticalAlign) {
			case AlignTop:
				rHorizontalAlign = AlignmentFlag.AlignRight;
				break;
			case AlignMiddle:
				rHorizontalAlign = AlignmentFlag.AlignCenter;
				break;
			case AlignBottom:
				rHorizontalAlign = AlignmentFlag.AlignLeft;
				break;
			default:
				break;
			}
		}
		double left = 0;
		double top = 0;
		switch (rHorizontalAlign) {
		case AlignLeft:
			left += margin;
			break;
		case AlignCenter:
			left -= width / 2;
			break;
		case AlignRight:
			left -= width + margin;
		default:
			break;
		}
		switch (rVerticalAlign) {
		case AlignTop:
			top += margin;
			break;
		case AlignMiddle:
			top -= height / 2;
			break;
		case AlignBottom:
			top -= height + margin;
			break;
		default:
			break;
		}
		WPen oldPen = painter.getPen();
		painter.setPen(this.textPen_.clone());
		WTransform oldTransform = painter.getWorldTransform().clone();
		painter.translate(pos);
		if (angle == 0) {
			painter.drawText(new WRectF(left, top, width, height),
					EnumSet.of(rHorizontalAlign, rVerticalAlign), text);
		} else {
			painter.rotate(-angle);
			painter.drawText(new WRectF(left, top, width, height),
					EnumSet.of(rHorizontalAlign, rVerticalAlign), text);
		}
		painter.setWorldTransform(oldTransform, false);
		painter.setPen(oldPen);
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * <p>
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	protected WPointF hv(double x, double y) {
		return this.hv(x, y, this.height_);
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * <p>
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	protected WPointF hv(final WPointF p) {
		if (p.isJavaScriptBound()) {
			if (this.getOrientation() == Orientation.Vertical) {
				return p;
			} else {
				return p.swapHV(this.height_);
			}
		}
		return this.hv(p.getX(), p.getY());
	}

	/**
	 * Conversion between chart and painter coordinates.
	 * <p>
	 * Converts from chart coordinates to painter coordinates, taking into
	 * account the chart orientation.
	 */
	protected WRectF hv(final WRectF r) {
		if (this.getOrientation() == Orientation.Vertical) {
			return r;
		} else {
			WPointF tl = this.hv(r.getBottomLeft());
			return new WRectF(tl.getX(), tl.getY(), r.getHeight(), r.getWidth());
		}
	}

	/**
	 * Returns the segment area for a combination of X and Y segments.
	 * <p>
	 * This segment area is used for clipping when rendering in a particular
	 * segment.
	 */
	protected WRectF chartSegmentArea(final WAxis yAxis, int xSegment,
			int ySegment) {
		final WAxis xAxis = this.getAxis(Axis.XAxis);
		final WAxis.Segment xs = xAxis.segments_.get(xSegment);
		final WAxis.Segment ys = yAxis.segments_.get(ySegment);
		double xRenderStart = xAxis.isInverted() ? xAxis.mapToDevice(
				xs.renderMaximum, xSegment) : xs.renderStart;
		double xRenderEnd = xAxis.isInverted() ? xAxis.mapToDevice(
				xs.renderMinimum, xSegment) : xs.renderStart + xs.renderLength;
		double yRenderStart = yAxis.isInverted() ? yAxis.mapToDevice(
				ys.renderMaximum, ySegment) : ys.renderStart;
		double yRenderEnd = yAxis.isInverted() ? yAxis.mapToDevice(
				ys.renderMinimum, ySegment) : ys.renderStart + ys.renderLength;
		double x1 = this.chartArea_.getLeft()
				+ xRenderStart
				+ (xSegment == 0 ? xs.renderMinimum == 0 ? 0 : -this
						.getAxisPadding() : -xAxis.getSegmentMargin() / 2);
		double x2 = this.chartArea_.getLeft()
				+ xRenderEnd
				+ (xSegment == xAxis.getSegmentCount() - 1 ? xs.renderMaximum == 0 ? 0
						: this.getAxisPadding()
						: xAxis.getSegmentMargin() / 2);
		double y1 = this.chartArea_.getBottom()
				- yRenderEnd
				- (ySegment == yAxis.getSegmentCount() - 1 ? ys.renderMaximum == 0 ? 0
						: this.getAxisPadding()
						: yAxis.getSegmentMargin() / 2);
		double y2 = this.chartArea_.getBottom()
				- yRenderStart
				+ (ySegment == 0 ? ys.renderMinimum == 0 ? 0 : this
						.getAxisPadding() : yAxis.getSegmentMargin() / 2);
		return new WRectF(Math.floor(x1 + 0.5), Math.floor(y1 + 0.5),
				Math.floor(x2 - x1), Math.floor(y2 - y1));
	}

	/**
	 * Calculates the chart area.
	 * <p>
	 * This calculates the chartArea(), which is the rectangle (in chart
	 * coordinates) that bounds the actual chart (thus excluding axes, labels,
	 * titles, legend, etc...).
	 * <p>
	 * 
	 * @see WAbstractChart#getPlotAreaPadding(Side side)
	 */
	protected void calcChartArea() {
		if (this.orientation_ == Orientation.Vertical) {
			this.chartArea_ = new WRectF(this.getPlotAreaPadding(Side.Left),
					this.getPlotAreaPadding(Side.Top), Math.max(10,
							this.width_ - this.getPlotAreaPadding(Side.Left)
									- this.getPlotAreaPadding(Side.Right)),
					Math.max(10,
							this.height_ - this.getPlotAreaPadding(Side.Top)
									- this.getPlotAreaPadding(Side.Bottom)));
		} else {
			this.chartArea_ = new WRectF(this.getPlotAreaPadding(Side.Top),
					this.getPlotAreaPadding(Side.Right), Math.max(10,
							this.width_ - this.getPlotAreaPadding(Side.Top)
									- this.getPlotAreaPadding(Side.Bottom)),
					Math.max(10,
							this.height_ - this.getPlotAreaPadding(Side.Right)
									- this.getPlotAreaPadding(Side.Left)));
		}
	}

	/**
	 * Prepares the axes for rendering.
	 * <p>
	 * Computes axis properties such as the range (if not manually specified),
	 * label interval (if not manually specified) and axis locations. These
	 * properties are stored within the axes.
	 * <p>
	 * 
	 * @see WCartesianChart#initLayout(WRectF rectangle, WPaintDevice device)
	 */
	protected boolean prepareAxes(WPaintDevice device) {
		if (this.yAxes_.isEmpty()) {
			return true;
		}
		final WAxis xAxis = this.getAxis(Axis.XAxis);
		Orientation yDir = this.orientation_;
		Orientation xDir = this.orientation_ == Orientation.Vertical ? Orientation.Horizontal
				: Orientation.Vertical;
		if (!xAxis.prepareRender(xDir, this.chartArea_.getWidth())) {
			return false;
		}
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			if (!this.yAxes_.get(i).axis.prepareRender(yDir,
					this.chartArea_.getHeight())) {
				return false;
			}
		}
		if (xAxis.getScale() == AxisScale.CategoryScale) {
			switch (xAxis.getLocation()) {
			case MinimumValue:
			case ZeroValue:
				this.xAxis_.location.loc = AxisValue.MinimumValue;
				break;
			case MaximumValue:
				this.xAxis_.location.loc = AxisValue.MaximumValue;
				break;
			case BothSides:
				this.xAxis_.location.loc = AxisValue.BothSides;
			}
		}
		AxisValue xLocation = this.xAxis_.axis.getLocation();
		if (xLocation == AxisValue.ZeroValue) {
			if (this.yAxes_.get(0).axis.segments_
					.get(this.yAxes_.get(0).axis.segments_.size() - 1).renderMaximum < 0) {
				xLocation = AxisValue.MaximumValue;
			} else {
				if (this.yAxes_.get(0).axis.segments_
						.get(this.yAxes_.get(0).axis.segments_.size() - 1).renderMinimum > 0) {
					xLocation = AxisValue.MinimumValue;
				} else {
					if (!this.yAxes_.get(0).axis.isOnAxis(0.0)) {
						xLocation = AxisValue.MinimumValue;
					}
				}
			}
		} else {
			if (xLocation == AxisValue.MinimumValue) {
				if (this.yAxes_.get(0).axis.segments_.get(0).renderMinimum == 0
						&& this.yAxes_.get(0).axis.getTickDirection() == TickDirection.Outwards) {
					xLocation = AxisValue.ZeroValue;
				}
			} else {
				if (xLocation == AxisValue.MaximumValue) {
					if (this.yAxes_.get(0).axis.segments_.get(this.yAxes_
							.get(0).axis.segments_.size() - 1).renderMaximum == 0) {
						xLocation = AxisValue.ZeroValue;
					}
				}
			}
		}
		this.xAxis_.location.loc = xLocation;
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.yAxes_.get(i).location.loc = this.yAxes_.get(i).axis
					.getLocation();
		}
		List<WAxis> minimumYaxes = this
				.collectYAxesAtLocation(AxisValue.MinimumValue);
		int offset = 0;
		for (int i = 0; i < minimumYaxes.size(); ++i) {
			final WAxis axis = minimumYaxes.get(i);
			if (axis.getLocation() != AxisValue.BothSides) {
				this.yAxes_.get(axis.getYAxisId()).location.loc = AxisValue.MinimumValue;
			}
			this.yAxes_.get(axis.getYAxisId()).location.minOffset = offset;
			this.yAxes_.get(axis.getYAxisId()).calculatedWidth = this
					.calcYAxisSize(axis, device) + 10;
			if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
				offset += 10;
			} else {
				offset += this.yAxes_.get(axis.getYAxisId()).calculatedWidth;
			}
		}
		List<WAxis> maximumYaxes = this
				.collectYAxesAtLocation(AxisValue.MaximumValue);
		offset = 0;
		for (int i = 0; i < maximumYaxes.size(); ++i) {
			final WAxis axis = maximumYaxes.get(i);
			if (axis.getLocation() != AxisValue.BothSides) {
				this.yAxes_.get(axis.getYAxisId()).location.loc = AxisValue.MaximumValue;
			}
			this.yAxes_.get(axis.getYAxisId()).location.maxOffset = offset;
			this.yAxes_.get(axis.getYAxisId()).calculatedWidth = this
					.calcYAxisSize(axis, device) + 10;
			if (i == 0 && axis.getTickDirection() == TickDirection.Inwards) {
				offset += 10;
			} else {
				offset += this.yAxes_.get(axis.getYAxisId()).calculatedWidth;
			}
		}
		return true;
	}

	/**
	 * Renders the background.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 */
	protected void renderBackground(final WPainter painter) {
		if (this.getBackground().getStyle() != BrushStyle.NoBrush) {
			painter.fillRect(this.hv(this.chartArea_), this.getBackground());
		}
	}

	/**
	 * Renders one or more properties of the axes.
	 * <p>
	 * This calls
	 * {@link WCartesianChart#renderAxis(WPainter painter, WAxis axis, EnumSet properties)
	 * renderAxis()} for each axis.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 */
	protected void renderAxes(final WPainter painter,
			EnumSet<AxisProperty> properties) {
		this.renderAxis(painter, this.xAxis_.axis, properties);
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			this.renderAxis(painter, this.yAxes_.get(i).axis, properties);
		}
	}

	/**
	 * Renders one or more properties of the axes.
	 * <p>
	 * Calls {@link #renderAxes(WPainter painter, EnumSet properties)
	 * renderAxes(painter, EnumSet.of(propertie, properties))}
	 */
	protected final void renderAxes(final WPainter painter,
			AxisProperty propertie, AxisProperty... properties) {
		renderAxes(painter, EnumSet.of(propertie, properties));
	}

	/**
	 * Renders the border of the chart area.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 * @see WCartesianChart#setBorderPen(WPen pen)
	 */
	protected void renderBorder(final WPainter painter) {
		WPainterPath area = new WPainterPath();
		int horizontalShift = 0;
		int verticalShift = 0;
		if (this.hasInwardsYAxisOnMaximumSide()) {
			horizontalShift = -1;
		}
		if (this.getAxis(Axis.XAxis).isVisible()
				&& this.getAxis(Axis.XAxis).getTickDirection() == TickDirection.Inwards) {
			verticalShift = -1;
		}
		area.addRect(this.hv(new WRectF(this.chartArea_.getLeft(),
				this.chartArea_.getTop(), this.chartArea_.getWidth()
						+ horizontalShift, this.chartArea_.getHeight()
						+ verticalShift)));
		painter.strokePath(area.getCrisp(), this.borderPen_);
	}

	/**
	 * Renders the curve labels.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 * @see WCartesianChart#addCurveLabel(CurveLabel label)
	 */
	protected void renderCurveLabels(final WPainter painter) {
		if (this.isInteractive()) {
			painter.save();
			WPainterPath clipPath = new WPainterPath();
			clipPath.addRect(this.hv(this.chartArea_));
			painter.setClipPath(clipPath);
			painter.setClipping(true);
		}
		for (int i = 0; i < this.curveLabels_.size(); ++i) {
			final CurveLabel label = this.curveLabels_.get(i);
			for (int j = 0; j < this.series_.size(); ++j) {
				final WDataSeries series = this.series_.get(j);
				if (series.isHidden()) {
					continue;
				}
				if (series == label.getSeries()) {
					WTransform t = this.zoomRangeTransform(series.getYAxis());
					if (series.getType() == SeriesType.LineSeries
							|| series.getType() == SeriesType.CurveSeries) {
						t.assign(t.multiply(this.curveTransform(series)));
					}
					int xSegment = 0;
					if (!this.isInteractive()) {
						while (xSegment < this.getAxis(Axis.XAxis)
								.getSegmentCount()
								&& (this.getAxis(Axis.XAxis).segments_
										.get(xSegment).renderMinimum > label
										.getPoint().getX() || this
										.getAxis(Axis.XAxis).segments_
										.get(xSegment).renderMaximum < label
										.getPoint().getX())) {
							++xSegment;
						}
					}
					int ySegment = 0;
					if (!this.isInteractive()) {
						while (ySegment < this.getAxis(series.getAxis())
								.getSegmentCount()
								&& (this.getAxis(series.getAxis()).segments_
										.get(ySegment).renderMinimum > label
										.getPoint().getY() || this
										.getAxis(series.getAxis()).segments_
										.get(ySegment).renderMaximum < label
										.getPoint().getY())) {
							++ySegment;
						}
					}
					if (xSegment < this.getAxis(Axis.XAxis).getSegmentCount()
							&& ySegment < this.getAxis(series.getAxis())
									.getSegmentCount()) {
						WPointF devicePoint = this.mapToDeviceWithoutTransform(
								label.getPoint().getX(), label.getPoint()
										.getY(), series.getAxis(), xSegment,
								ySegment);
						WTransform translation = new WTransform().translate(t
								.map(devicePoint));
						painter.save();
						painter.setWorldTransform(translation);
						label.render(painter);
						painter.restore();
					}
				}
			}
		}
		if (this.isInteractive()) {
			painter.restore();
		}
	}

	/**
	 * Renders all series data, including value labels.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 */
	protected void renderSeries(final WPainter painter) {
		if (this.isInteractive()) {
			painter.save();
			WPainterPath clipPath = new WPainterPath();
			clipPath.addRect(this.hv(this.chartArea_));
			painter.setClipPath(clipPath);
			painter.setClipping(true);
		}
		this.barTooltips_.clear();
		{
			SeriesRenderIterator iterator = new SeriesRenderIterator(this,
					painter);
			this.iterateSeries(iterator, painter, true);
		}
		{
			LabelRenderIterator iterator = new LabelRenderIterator(this,
					painter);
			this.iterateSeries(iterator, painter);
		}
		{
			MarkerRenderIterator iterator = new MarkerRenderIterator(this,
					painter);
			this.iterateSeries(iterator, painter);
		}
		if (this.isInteractive()) {
			painter.restore();
		}
	}

	/**
	 * Renders the (default) legend and chart titles.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 */
	protected void renderLegend(final WPainter painter) {
		boolean vertical = this.getOrientation() == Orientation.Vertical;
		int w = vertical ? this.width_ : this.height_;
		int h = vertical ? this.height_ : this.width_;
		int margin;
		if (this.isLegendEnabled()) {
			painter.save();
			WPaintDevice device = painter.getDevice();
			WAxis caxis = null;
			Orientation titleOrientation = Orientation.Horizontal;
			if (this.getLegendSide() == Side.Right) {
				if (this.yAxes_.get(1).axis.isVisible()) {
					caxis = this.yAxes_.get(1).axis;
				} else {
					if (this.yAxes_.get(0).axis.isVisible()
							&& (this.yAxes_.get(0).axis.getLocation() == AxisValue.BothSides || this.yAxes_
									.get(0).axis.getLocation() == AxisValue.MaximumValue)) {
						caxis = this.yAxes_.get(0).axis;
					}
				}
				if (caxis != null
						&& caxis.getTitleOrientation() == Orientation.Vertical) {
					titleOrientation = Orientation.Vertical;
				}
			} else {
				if (this.getLegendSide() == Side.Left) {
					caxis = this.yAxes_.get(0).axis;
					if (caxis.getTitleOrientation() == Orientation.Vertical) {
						titleOrientation = Orientation.Vertical;
					}
				}
			}
			boolean fontMetrics = !EnumUtils.mask(device.getFeatures(),
					WPaintDevice.FeatureFlag.HasFontMetrics).isEmpty();
			if (titleOrientation == Orientation.Vertical && caxis != null) {
				if (fontMetrics) {
					margin = (int) (caxis.calcTitleSize(device,
							Orientation.Vertical) + this.yAxes_.get(1).axis
							.calcMaxTickLabelSize(device,
									Orientation.Horizontal));
				} else {
					margin = 30;
				}
			} else {
				margin = 20;
			}
			if (caxis != null && titleOrientation == Orientation.Horizontal) {
				if (fontMetrics) {
					margin += caxis.calcMaxTickLabelSize(device,
							Orientation.Horizontal);
				} else {
					margin += 20;
				}
			}
			int numSeriesWithLegend = 0;
			for (int i = 0; i < this.getSeries().size(); ++i) {
				if (this.getSeries().get(i).isLegendEnabled()) {
					++numSeriesWithLegend;
				}
			}
			painter.setFont(this.getLegendFont());
			WFont f = painter.getFont();
			if (this.isAutoLayoutEnabled()
					&& !EnumUtils.mask(painter.getDevice().getFeatures(),
							WPaintDevice.FeatureFlag.HasFontMetrics).isEmpty()) {
				int columnWidth = 0;
				for (int i = 0; i < this.getSeries().size(); ++i) {
					if (this.getSeries().get(i).isLegendEnabled()) {
						WString s = this
								.getSeries()
								.get(i)
								.getModel()
								.getHeaderData(
										this.getSeries().get(i)
												.getModelColumn());
						WTextItem t = painter.getDevice().measureText(s);
						columnWidth = Math.max(columnWidth, (int) t.getWidth());
					}
				}
				columnWidth += 25;
				WCartesianChart self = this;
				self.legend_.setLegendColumnWidth(new WLength(columnWidth));
				if (this.getLegendSide() == Side.Top
						|| this.getLegendSide() == Side.Bottom) {
					self.legend_.setLegendColumns(Math.max(1, w / columnWidth
							- 1));
				}
			}
			int numLegendRows = (numSeriesWithLegend - 1)
					/ this.getLegendColumns() + 1;
			double lineHeight = f.getSizeLength().toPixels() * 1.5;
			int legendWidth = (int) this.getLegendColumnWidth().toPixels()
					* Math.min(this.getLegendColumns(), numSeriesWithLegend);
			int legendHeight = (int) (numLegendRows * lineHeight);
			int x = 0;
			int y = 0;
			switch (this.getLegendSide()) {
			case Left:
				if (this.getLegendLocation() == LegendLocation.LegendInside) {
					x = this.getPlotAreaPadding(Side.Left) + margin;
				} else {
					x = this.getPlotAreaPadding(Side.Left) - margin
							- legendWidth;
				}
				break;
			case Right:
				x = w - this.getPlotAreaPadding(Side.Right);
				if (this.getLegendLocation() == LegendLocation.LegendInside) {
					x -= margin + legendWidth;
				} else {
					x += margin;
				}
				break;
			case Top:
				if (this.getLegendLocation() == LegendLocation.LegendInside) {
					y = this.getPlotAreaPadding(Side.Top) + margin;
				} else {
					y = this.getPlotAreaPadding(Side.Top) - margin
							- legendHeight;
				}
				break;
			case Bottom:
				y = h - this.getPlotAreaPadding(Side.Bottom);
				if (this.getLegendLocation() == LegendLocation.LegendInside) {
					y -= margin + legendHeight;
				} else {
					y += margin;
				}
			default:
				break;
			}
			switch (this.getLegendAlignment()) {
			case AlignTop:
				y = this.getPlotAreaPadding(Side.Top) + margin;
				break;
			case AlignMiddle: {
				double middle = this.getPlotAreaPadding(Side.Top)
						+ (h - this.getPlotAreaPadding(Side.Top) - this
								.getPlotAreaPadding(Side.Bottom)) / 2;
				y = (int) (middle - legendHeight / 2);
			}
				break;
			case AlignBottom:
				y = h - this.getPlotAreaPadding(Side.Bottom) - margin
						- legendHeight;
				break;
			case AlignLeft:
				x = this.getPlotAreaPadding(Side.Left) + margin;
				break;
			case AlignCenter: {
				double center = this.getPlotAreaPadding(Side.Left)
						+ (w - this.getPlotAreaPadding(Side.Left) - this
								.getPlotAreaPadding(Side.Right)) / 2;
				x = (int) (center - legendWidth / 2);
			}
				break;
			case AlignRight:
				x = w - this.getPlotAreaPadding(Side.Right) - margin
						- legendWidth;
				break;
			default:
				break;
			}
			if (this.getLegendLocation() == LegendLocation.LegendOutside) {
				if (this.getLegendSide() == Side.Top && !vertical
						&& this.getAxis(Axis.Y1Axis).isVisible()) {
					y -= 16;
				}
				if (this.getLegendSide() == Side.Right
						&& vertical
						&& (this.getAxis(Axis.Y2Axis).isVisible() || this
								.getAxis(Axis.Y1Axis).isVisible()
								&& (this.getAxis(Axis.Y1Axis).getLocation() == AxisValue.BothSides || this
										.getAxis(Axis.Y1Axis).getLocation() == AxisValue.MaximumValue))) {
					x += 40;
				}
				if (this.getLegendSide() == Side.Right
						&& !vertical
						&& this.getAxis(Axis.XAxis).isVisible()
						&& (this.getAxis(Axis.XAxis).getLocation() == AxisValue.MaximumValue || this
								.getAxis(Axis.XAxis).getLocation() == AxisValue.BothSides)) {
					x += 40;
				}
				if (this.getLegendSide() == Side.Bottom
						&& (vertical && this.getAxis(Axis.XAxis).isVisible() || !vertical
								&& (this.getAxis(Axis.Y2Axis).isVisible() || this
										.getAxis(Axis.Y1Axis).isVisible()
										&& (this.getAxis(Axis.Y1Axis)
												.getLocation() == AxisValue.BothSides || this
												.getAxis(Axis.Y1Axis)
												.getLocation() == AxisValue.MaximumValue)))) {
					y += 16;
				}
				if (this.getLegendSide() == Side.Left
						&& (vertical && this.getAxis(Axis.Y1Axis).isVisible() || !vertical
								&& this.getAxis(Axis.XAxis).isVisible())) {
					x -= 40;
				}
			}
			painter.setPen(this.getLegendBorder().clone());
			painter.setBrush(this.getLegendBackground());
			painter.drawRect(x - margin / 2, y - margin / 2, legendWidth
					+ margin, legendHeight + margin);
			painter.setPen(new WPen());
			painter.setFont(this.getLegendFont());
			int item = 0;
			for (int i = 0; i < this.getSeries().size(); ++i) {
				if (this.getSeries().get(i).isLegendEnabled()) {
					int col = item % this.getLegendColumns();
					int row = item / this.getLegendColumns();
					double itemX = x + col
							* this.getLegendColumnWidth().toPixels();
					double itemY = y + row * lineHeight;
					this.renderLegendItem(painter, new WPointF(itemX, itemY
							+ lineHeight / 2), this.getSeries().get(i));
					++item;
				}
			}
			painter.restore();
		}
		if (!(this.getTitle().length() == 0)) {
			int x = this.getPlotAreaPadding(Side.Left)
					+ (w - this.getPlotAreaPadding(Side.Left) - this
							.getPlotAreaPadding(Side.Right)) / 2;
			painter.save();
			painter.setFont(this.getTitleFont());
			double titleHeight = this.getTitleFont().getSizeLength().toPixels();
			final int TITLE_PADDING = 10;
			painter.drawText(x - 500, this.getPlotAreaPadding(Side.Top)
					- titleHeight - TITLE_PADDING, 1000, titleHeight, EnumSet
					.of(AlignmentFlag.AlignCenter, AlignmentFlag.AlignTop),
					this.getTitle());
			painter.restore();
		}
	}

	/**
	 * Renders properties of one axis.
	 * <p>
	 * 
	 * @see WCartesianChart#renderAxes(WPainter painter, EnumSet properties)
	 */
	protected void renderAxis(final WPainter painter, final WAxis axis,
			EnumSet<AxisProperty> properties) {
		if (!axis.isVisible()) {
			return;
		}
		boolean isYAxis = axis.getId() != Axis.XAxis;
		final AxisValue location = axis.getId() == Axis.XAxis ? this.xAxis_.location.loc
				: this.yAxes_.get(axis.getYAxisId()).location.loc;
		if (this.isInteractive()
				&& ((painter.getDevice()) instanceof WCanvasPaintDevice ? (WCanvasPaintDevice) (painter
						.getDevice()) : null) != null) {
			WRectF clipRect = null;
			WRectF area = this.hv(this.chartArea_);
			if (axis.getLocation() == AxisValue.ZeroValue
					&& location == AxisValue.ZeroValue) {
				clipRect = area;
			} else {
				if (isYAxis != (this.getOrientation() == Orientation.Horizontal)) {
					double h = area.getHeight();
					if (this.xAxis_.location.loc == AxisValue.ZeroValue
							&& this.getOrientation() == Orientation.Vertical) {
						h += 1;
					}
					clipRect = new WRectF(0.0, area.getTop(),
							isYAxis ? this.width_ : this.height_, h);
				} else {
					clipRect = new WRectF(area.getLeft(), 0.0, area.getWidth(),
							isYAxis ? this.height_ : this.width_);
				}
			}
			if (properties.equals(AxisProperty.Labels)) {
				clipRect = new WRectF(clipRect.getLeft() - 1,
						clipRect.getTop() - 1, clipRect.getWidth() + 2,
						clipRect.getHeight() + 2);
			}
			WPainterPath clipPath = new WPainterPath();
			clipPath.addRect(clipRect);
			painter.save();
			painter.setClipPath(clipPath);
			painter.setClipping(true);
		}
		List<AxisValue> locations = new ArrayList<AxisValue>();
		if (location == AxisValue.BothSides) {
			locations.add(AxisValue.MinimumValue);
			locations.add(AxisValue.MaximumValue);
		} else {
			locations.add(location);
		}
		for (int l = 0; l < locations.size(); ++l) {
			WPointF axisStart = new WPointF();
			WPointF axisEnd = new WPointF();
			double tickStart = 0.0;
			double tickEnd = 0.0;
			double labelPos = 0.0;
			AlignmentFlag labelHFlag = AlignmentFlag.AlignCenter;
			AlignmentFlag labelVFlag = AlignmentFlag.AlignMiddle;
			if (isYAxis) {
				labelVFlag = AlignmentFlag.AlignMiddle;
				axisStart.setY(this.chartArea_.getBottom());
				axisEnd.setY(this.chartArea_.getTop());
			} else {
				labelHFlag = AlignmentFlag.AlignCenter;
				axisStart.setX(this.chartArea_.getLeft());
				axisEnd.setX(this.chartArea_.getRight());
			}
			switch (locations.get(l)) {
			case MinimumValue:
				if (isYAxis) {
					double x = this.chartArea_.getLeft()
							- this.yAxes_.get(axis.getYAxisId()).location.minOffset;
					if (axis.getTickDirection() == TickDirection.Inwards) {
						tickStart = 0;
						tickEnd = TICK_LENGTH;
						labelPos = TICK_LENGTH;
						labelHFlag = AlignmentFlag.AlignLeft;
						axisStart.setX(x);
						axisEnd.setX(x);
					} else {
						tickStart = -TICK_LENGTH;
						tickEnd = 0;
						labelPos = -TICK_LENGTH;
						labelHFlag = AlignmentFlag.AlignRight;
						x -= axis.getMargin();
						axisStart.setX(x);
						axisEnd.setX(x);
					}
				} else {
					if (axis.getTickDirection() == TickDirection.Inwards) {
						tickStart = -TICK_LENGTH;
						tickEnd = 0;
						labelPos = -TICK_LENGTH;
						labelVFlag = AlignmentFlag.AlignBottom;
						double y = this.chartArea_.getBottom() - 1;
						axisStart.setY(y);
						axisEnd.setY(y);
					} else {
						tickStart = 0;
						tickEnd = TICK_LENGTH;
						labelPos = TICK_LENGTH;
						labelVFlag = AlignmentFlag.AlignTop;
						double y = this.chartArea_.getBottom()
								+ axis.getMargin();
						axisStart.setY(y);
						axisEnd.setY(y);
					}
				}
				break;
			case MaximumValue:
				if (isYAxis) {
					double x = this.chartArea_.getRight()
							+ this.yAxes_.get(axis.getYAxisId()).location.maxOffset;
					if (axis.getTickDirection() == TickDirection.Inwards) {
						tickStart = -TICK_LENGTH;
						tickEnd = 0;
						labelPos = -TICK_LENGTH;
						labelHFlag = AlignmentFlag.AlignRight;
						x -= 1;
						axisStart.setX(x);
						axisEnd.setX(x);
					} else {
						tickStart = 0;
						tickEnd = TICK_LENGTH;
						labelPos = TICK_LENGTH;
						labelHFlag = AlignmentFlag.AlignLeft;
						x += axis.getMargin();
						axisStart.setX(x);
						axisEnd.setX(x);
					}
				} else {
					if (axis.getTickDirection() == TickDirection.Inwards) {
						tickStart = 0;
						tickEnd = TICK_LENGTH;
						labelPos = TICK_LENGTH;
						labelVFlag = AlignmentFlag.AlignTop;
						double y = this.chartArea_.getTop();
						axisStart.setY(y);
						axisEnd.setY(y);
					} else {
						tickStart = -TICK_LENGTH;
						tickEnd = 0;
						labelPos = -TICK_LENGTH;
						labelVFlag = AlignmentFlag.AlignBottom;
						double y = this.chartArea_.getTop() - axis.getMargin();
						axisStart.setY(y);
						axisEnd.setY(y);
					}
				}
				break;
			case ZeroValue:
				tickStart = -TICK_LENGTH;
				tickEnd = TICK_LENGTH;
				if (isYAxis) {
					double x = this.chartArea_.getLeft()
							+ this.getAxis(Axis.XAxis).mapToDevice(0.0);
					axisStart.setX(x);
					axisEnd.setX(x);
					labelHFlag = AlignmentFlag.AlignRight;
					if (this.getType() == ChartType.CategoryChart) {
						labelPos = this.chartArea_.getLeft() - axisStart.getX()
								- TICK_LENGTH;
					} else {
						labelPos = -TICK_LENGTH;
					}
				} else {
					double y = this.chartArea_.getBottom()
							- this.getAxis(Axis.YAxis).mapToDevice(0.0);
					axisStart.setY(y);
					axisEnd.setY(y);
					labelVFlag = AlignmentFlag.AlignTop;
					if (this.getType() == ChartType.CategoryChart) {
						labelPos = this.chartArea_.getBottom()
								- axisStart.getY() + TICK_LENGTH;
					} else {
						labelPos = TICK_LENGTH;
					}
				}
				break;
			case BothSides:
				assert false;
				break;
			}
			if (!EnumUtils.mask(properties, AxisProperty.Labels).isEmpty()
					&& !(axis.getTitle().length() == 0)) {
				if (this.isInteractive()) {
					painter.setClipping(false);
				}
				WFont oldFont2 = painter.getFont();
				WFont titleFont = axis.getTitleFont();
				painter.setFont(titleFont);
				boolean chartVertical = this.getOrientation() == Orientation.Vertical;
				if (isYAxis) {
					double u = axisStart.getX();
					if (chartVertical) {
						if (axis.getTitleOrientation() == Orientation.Horizontal) {
							this.renderLabel(
									painter,
									axis.getTitle(),
									new WPointF(
											u
													+ (labelHFlag == AlignmentFlag.AlignRight ? 15
															: -15),
											this.chartArea_.getTop() - 8),
									EnumSet.of(labelHFlag,
											AlignmentFlag.AlignBottom), 0, 10);
						} else {
							WPaintDevice device = painter.getDevice();
							double size = 0;
							double titleSizeW = 0;
							if (!EnumUtils.mask(device.getFeatures(),
									WPaintDevice.FeatureFlag.HasFontMetrics)
									.isEmpty()) {
								if (axis.getTickDirection() == TickDirection.Outwards) {
									size = axis.calcMaxTickLabelSize(device,
											Orientation.Horizontal);
								}
								titleSizeW = axis.calcTitleSize(device,
										Orientation.Vertical);
								if (axis.getTickDirection() == TickDirection.Inwards) {
									titleSizeW = -titleSizeW;
								}
							} else {
								size = 35;
								if (axis.getTickDirection() == TickDirection.Inwards) {
									size = -20;
								}
							}
							this.renderLabel(
									painter,
									axis.getTitle(),
									new WPointF(
											u
													+ (labelHFlag == AlignmentFlag.AlignRight ? -(size
															+ titleSizeW + 5)
															: +(size
																	+ titleSizeW + 5)),
											this.chartArea_.getCenter().getY()),
									EnumSet.of(AlignmentFlag.AlignCenter,
											AlignmentFlag.AlignMiddle),
									locations.get(l) == AxisValue.MaximumValue ? -90
											: 90, 10);
						}
					} else {
						double extraMargin = 0;
						WPaintDevice device = painter.getDevice();
						if (axis.getTickDirection() == TickDirection.Outwards) {
							if (!EnumUtils.mask(device.getFeatures(),
									WPaintDevice.FeatureFlag.HasFontMetrics)
									.isEmpty()) {
								extraMargin = axis.calcMaxTickLabelSize(device,
										Orientation.Vertical);
							} else {
								extraMargin = 15;
							}
						}
						if (locations.get(l) != AxisValue.MaximumValue) {
							extraMargin = -extraMargin;
						}
						EnumSet<AlignmentFlag> alignment = EnumSet
								.of(locations.get(l) == AxisValue.MaximumValue ? AlignmentFlag.AlignLeft
										: AlignmentFlag.AlignRight,
										AlignmentFlag.AlignMiddle);
						this.renderLabel(painter, axis.getTitle(), new WPointF(
								u + extraMargin, this.chartArea_.getCenter()
										.getY()), alignment, 0, 10);
					}
				} else {
					double u = axisStart.getY();
					if (chartVertical) {
						double extraMargin = 0;
						WPaintDevice device = painter.getDevice();
						if (!EnumUtils.mask(device.getFeatures(),
								WPaintDevice.FeatureFlag.HasFontMetrics)
								.isEmpty()) {
							if (axis.getTickDirection() == TickDirection.Outwards) {
								extraMargin = axis.calcMaxTickLabelSize(device,
										Orientation.Vertical);
							}
						} else {
							if (axis.getTickDirection() == TickDirection.Outwards) {
								extraMargin = 15;
							}
						}
						if (locations.get(l) == AxisValue.MaximumValue) {
							extraMargin = -extraMargin;
						}
						EnumSet<AlignmentFlag> alignment = EnumSet
								.of(locations.get(l) == AxisValue.MaximumValue ? AlignmentFlag.AlignBottom
										: AlignmentFlag.AlignTop,
										AlignmentFlag.AlignCenter);
						this.renderLabel(painter, axis.getTitle(), new WPointF(
								this.chartArea_.getCenter().getX(), u
										+ extraMargin), alignment, 0, 10);
					} else {
						if (axis.getTitleOrientation() == Orientation.Vertical) {
							WPaintDevice device = painter.getDevice();
							double extraMargin = 0;
							if (!EnumUtils.mask(device.getFeatures(),
									WPaintDevice.FeatureFlag.HasFontMetrics)
									.isEmpty()) {
								if (axis.getTickDirection() == TickDirection.Outwards) {
									extraMargin = axis.calcMaxTickLabelSize(
											device, Orientation.Horizontal);
								}
								extraMargin += axis.calcTitleSize(device,
										Orientation.Vertical);
							} else {
								extraMargin = 40;
							}
							if (locations.get(l) == AxisValue.MaximumValue) {
								extraMargin = -extraMargin;
							}
							this.renderLabel(
									painter,
									axis.getTitle(),
									new WPointF(this.chartArea_.getCenter()
											.getX(), u + extraMargin),
									EnumSet.of(AlignmentFlag.AlignMiddle,
											AlignmentFlag.AlignCenter),
									locations.get(l) == AxisValue.MaximumValue ? -90
											: 90, 10);
						} else {
							EnumSet<AlignmentFlag> alignment = EnumSet
									.of(locations.get(l) == AxisValue.MaximumValue ? AlignmentFlag.AlignBottom
											: AlignmentFlag.AlignTop,
											AlignmentFlag.AlignLeft);
							this.renderLabel(painter, axis.getTitle(),
									new WPointF(this.chartArea_.getRight(), u),
									alignment, 0, 8);
						}
					}
				}
				painter.setFont(oldFont2);
				if (this.isInteractive()) {
					painter.setClipping(true);
				}
			}
			final double ANGLE1 = 15;
			final double ANGLE2 = 80;
			if (isYAxis) {
				if (axis.getLabelAngle() > ANGLE1) {
					labelVFlag = labelPos < 0 ? AlignmentFlag.AlignBottom
							: AlignmentFlag.AlignTop;
					if (axis.getLabelAngle() > ANGLE2) {
						labelHFlag = AlignmentFlag.AlignCenter;
					}
				} else {
					if (axis.getLabelAngle() < -ANGLE1) {
						labelVFlag = labelPos < 0 ? AlignmentFlag.AlignTop
								: AlignmentFlag.AlignBottom;
						if (axis.getLabelAngle() < -ANGLE2) {
							labelHFlag = AlignmentFlag.AlignCenter;
						}
					}
				}
			} else {
				if (axis.getLabelAngle() > ANGLE1) {
					labelHFlag = labelPos > 0 ? AlignmentFlag.AlignRight
							: AlignmentFlag.AlignLeft;
					if (axis.getLabelAngle() > ANGLE2) {
						labelVFlag = AlignmentFlag.AlignMiddle;
					}
				} else {
					if (axis.getLabelAngle() < -ANGLE1) {
						labelHFlag = labelPos > 0 ? AlignmentFlag.AlignLeft
								: AlignmentFlag.AlignRight;
						if (axis.getLabelAngle() < -ANGLE2) {
							labelVFlag = AlignmentFlag.AlignMiddle;
						}
					}
				}
			}
			if (this.getOrientation() == Orientation.Horizontal) {
				axisStart = this.hv(axisStart);
				axisEnd = this.hv(axisEnd);
				AlignmentFlag rHFlag = AlignmentFlag.AlignCenter;
				AlignmentFlag rVFlag = AlignmentFlag.AlignMiddle;
				switch (labelHFlag) {
				case AlignLeft:
					rVFlag = AlignmentFlag.AlignTop;
					break;
				case AlignCenter:
					rVFlag = AlignmentFlag.AlignMiddle;
					break;
				case AlignRight:
					rVFlag = AlignmentFlag.AlignBottom;
					break;
				default:
					break;
				}
				switch (labelVFlag) {
				case AlignTop:
					rHFlag = AlignmentFlag.AlignRight;
					break;
				case AlignMiddle:
					rHFlag = AlignmentFlag.AlignCenter;
					break;
				case AlignBottom:
					rHFlag = AlignmentFlag.AlignLeft;
					break;
				default:
					break;
				}
				labelHFlag = rHFlag;
				labelVFlag = rVFlag;
				boolean invertTicks = !isYAxis;
				if (invertTicks) {
					tickStart = -tickStart;
					tickEnd = -tickEnd;
					labelPos = -labelPos;
				}
			}
			List<WPen> pens = new ArrayList<WPen>();
			List<WPen> textPens = new ArrayList<WPen>();
			if (this.isInteractive()) {
				final List<WCartesianChart.PenAssignment> assignment = axis
						.getId() == Axis.XAxis ? this.xAxis_.pens : this.yAxes_
						.get(axis.getYAxisId()).pens;
				if (!assignment.isEmpty()) {
					for (int i = 0; i < assignment.size(); ++i) {
						pens.add(assignment.get(i).pen.getValue());
						textPens.add(assignment.get(i).textPen.getValue());
					}
				}
			}
			WTransform transform = new WTransform();
			WRectF area = this.hv(this.chartArea_);
			if (axis.getLocation() == AxisValue.ZeroValue) {
				transform.assign(new WTransform(1, 0, 0, -1, area.getLeft(),
						area.getBottom())
						.multiply(this.xAxis_.transform)
						.multiply(this.yAxes_.get(axis.getYAxisId()).transform)
						.multiply(
								new WTransform(1, 0, 0, -1, -area.getLeft(),
										area.getBottom())));
			} else {
				if (isYAxis && this.getOrientation() == Orientation.Vertical) {
					transform.assign(new WTransform(1, 0, 0, -1, 0, area
							.getBottom()).multiply(
							this.yAxes_.get(axis.getYAxisId()).transform)
							.multiply(
									new WTransform(1, 0, 0, -1, 0, area
											.getBottom())));
				} else {
					if (isYAxis
							&& this.getOrientation() == Orientation.Horizontal) {
						transform.assign(new WTransform(0, 1, 1, 0, area
								.getLeft(), 0).multiply(
								this.yAxes_.get(axis.getYAxisId()).transform)
								.multiply(
										new WTransform(0, 1, 1, 0, 0, -area
												.getLeft())));
					} else {
						if (this.getOrientation() == Orientation.Horizontal) {
							transform.assign(new WTransform(0, 1, 1, 0, 0, area
									.getTop()).multiply(this.xAxis_.transform)
									.multiply(
											new WTransform(0, 1, 1, 0, -area
													.getTop(), 0)));
						} else {
							transform.assign(new WTransform(1, 0, 0, 1, area
									.getLeft(), 0).multiply(
									this.xAxis_.transform).multiply(
									new WTransform(1, 0, 0, 1, -area.getLeft(),
											0)));
						}
					}
				}
			}
			AxisValue side = location == AxisValue.BothSides ? locations.get(l)
					: axis.getLocation();
			axis.render(painter, properties, axisStart, axisEnd, tickStart,
					tickEnd, labelPos, EnumSet.of(labelHFlag, labelVFlag),
					transform, side, pens, textPens);
		}
		if (this.isInteractive()) {
			painter.restore();
		}
	}

	/**
	 * Renders properties of one axis.
	 * <p>
	 * Calls {@link #renderAxis(WPainter painter, WAxis axis, EnumSet properties)
	 * renderAxis(painter, axis, EnumSet.of(propertie, properties))}
	 */
	protected final void renderAxis(final WPainter painter, final WAxis axis,
			AxisProperty propertie, AxisProperty... properties) {
		renderAxis(painter, axis, EnumSet.of(propertie, properties));
	}

	/**
	 * Renders grid lines along the ticks of the given axis.
	 * <p>
	 * 
	 * @see WCartesianChart#render(WPainter painter, WRectF rectangle)
	 */
	protected void renderGrid(final WPainter painter, final WAxis ax) {
		if (!ax.isGridLinesEnabled()) {
			return;
		}
		boolean isYAxis = ax.getId() != Axis.XAxis;
		final WAxis other = isYAxis ? this.getAxis(Axis.XAxis) : this
				.getAxis(Axis.Y1Axis);
		final WAxis.Segment s0 = other.segments_.get(0);
		final WAxis.Segment sn = other.segments_
				.get(other.segments_.size() - 1);
		double ou0 = s0.renderStart;
		double oun = sn.renderStart + sn.renderLength;
		if (!isYAxis) {
			ou0 = this.chartArea_.getBottom() - ou0;
			oun = this.chartArea_.getBottom() - oun;
		} else {
			ou0 = this.chartArea_.getLeft() + ou0;
			oun = this.chartArea_.getLeft() + oun;
		}
		if (this.isInteractive()) {
			painter.save();
			WPainterPath clipPath = new WPainterPath();
			clipPath.addRect(this.hv(this.chartArea_));
			painter.setClipPath(clipPath);
			painter.setClipping(true);
		}
		List<WPen> pens = new ArrayList<WPen>();
		final List<WCartesianChart.PenAssignment> assignments = ax.getId() == Axis.XAxis ? this.xAxis_.pens
				: this.yAxes_.get(ax.getYAxisId()).pens;
		if (assignments.isEmpty()) {
			pens.add(ax.getGridLinesPen());
		} else {
			for (int i = 0; i < assignments.size(); ++i) {
				pens.add(assignments.get(i).gridPen.getValue());
			}
		}
		AxisConfig axisConfig = new AxisConfig();
		if (ax.getLocation() == AxisValue.BothSides) {
			axisConfig.side = AxisValue.MinimumValue;
		} else {
			axisConfig.side = ax.getLocation();
		}
		for (int level = 1; level <= pens.size(); ++level) {
			WPainterPath gridPath = new WPainterPath();
			axisConfig.zoomLevel = level;
			List<Double> gridPos = ax.gridLinePositions(axisConfig);
			for (int i = 0; i < gridPos.size(); ++i) {
				double u = gridPos.get(i);
				if (isYAxis) {
					u = this.chartArea_.getBottom() - u;
					gridPath.moveTo(this.hv(ou0, u));
					gridPath.lineTo(this.hv(oun, u));
				} else {
					u = this.chartArea_.getLeft() + u;
					gridPath.moveTo(this.hv(u, ou0));
					gridPath.lineTo(this.hv(u, oun));
				}
			}
			painter.strokePath(
					this.zoomRangeTransform(isYAxis ? ax.getYAxisId() : 0)
							.map(gridPath).getCrisp(), pens.get(level - 1));
		}
		if (this.isInteractive()) {
			painter.restore();
		}
	}

	/**
	 * Renders other, user-defined things.
	 * <p>
	 * The default implementation sets the painter&apos;s
	 * {@link WPainter#setClipPath(WPainterPath clipPath) clip path} to the
	 * chart area, but does not enable clipping.
	 * <p>
	 * This method can be overridden to draw extra content onto the chart.
	 * <p>
	 * {@link } coordinates can be mapped to device coordinates with
	 * {@link WCartesianChart#mapToDeviceWithoutTransform(Object xValue, Object yValue, Axis ordinateAxis, int xSegment, int ySegment)
	 * mapToDeviceWithoutTransform()}. If these need to move and scale along
	 * with the zoom range, those points can be transformed with
	 * {@link WCartesianChart#zoomRangeTransform(int yAxis)
	 * zoomRangeTransform()}.
	 * <p>
	 * This method is called last by default. If you want to render other things
	 * at some other moment, you can override render(WPainter&amp;, const
	 * WRectF&amp;).
	 */
	protected void renderOther(final WPainter painter) {
		WPainterPath clipPath = new WPainterPath();
		clipPath.addRect(this.chartArea_);
		painter.setClipPath(clipPath);
	}

	/**
	 * Calculates the total number of bar groups.
	 */
	protected int getCalcNumBarGroups() {
		int numBarGroups = 0;
		boolean newGroup = true;
		for (int i = 0; i < this.getSeries().size(); ++i) {
			if (this.getSeries().get(i).getType() == SeriesType.BarSeries) {
				if (newGroup || !this.getSeries().get(i).isStacked()) {
					++numBarGroups;
				}
				newGroup = false;
			} else {
				newGroup = true;
			}
		}
		return numBarGroups;
	}

	protected void render(EnumSet<RenderFlag> flags) {
		super.render(flags);
		if (!EnumUtils.mask(flags, RenderFlag.RenderFull).isEmpty()
				|| !this.jsDefined_) {
			this.defineJavaScript();
		}
	}

	protected void setFormData(final WObject.FormData formData) {
		super.setFormData(formData);
		final WTransform xTransform = this.xAxis_.transformHandle.getValue();
		List<WTransform> yTransforms = new ArrayList<WTransform>();
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			yTransforms.add(this.yAxes_.get(i).transformHandle.getValue());
		}
		if (!this.getAxis(Axis.XAxis).zoomRangeDirty_) {
			WPointF devicePan = new WPointF(xTransform.getDx()
					/ xTransform.getM11(), 0.0);
			WPointF modelPan = new WPointF(this.getAxis(Axis.XAxis)
					.mapFromDevice(-devicePan.getX()), 0.0);
			if (xTransform.isIdentity()) {
				this.getAxis(Axis.XAxis).setZoomRangeFromClient(
						WAxis.AUTO_MINIMUM, WAxis.AUTO_MAXIMUM);
			} else {
				double z = xTransform.getM11();
				double x = modelPan.getX();
				double min = this.getAxis(Axis.XAxis).mapFromDevice(0.0);
				double max = this.getAxis(Axis.XAxis).mapFromDevice(
						this.getAxis(Axis.XAxis).fullRenderLength_);
				double x2 = x + (max - min) / z;
				this.getAxis(Axis.XAxis).setZoomRangeFromClient(x, x2);
			}
		}
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			if (!this.yAxis(i).zoomRangeDirty_) {
				WPointF devicePan = new WPointF(0.0, yTransforms.get(i).getDy()
						/ yTransforms.get(i).getM22());
				WPointF modelPan = new WPointF(0.0, this.yAxis(i)
						.mapFromDevice(-devicePan.getY()));
				if (yTransforms.get(i).isIdentity()) {
					this.yAxis(i).setZoomRangeFromClient(WAxis.AUTO_MINIMUM,
							WAxis.AUTO_MAXIMUM);
				} else {
					double z = yTransforms.get(i).getM22();
					double y = modelPan.getY();
					double min = this.yAxis(i).mapFromDevice(0.0);
					double max = this.yAxis(i).mapFromDevice(
							this.yAxis(i).fullRenderLength_);
					double y2 = y + (max - min) / z;
					this.yAxis(i).setZoomRangeFromClient(y, y2);
				}
			}
		}
		if (this.curveTransforms_.size() != 0) {
			for (int i = 0; i < this.series_.size(); ++i) {
				final WDataSeries s = this.series_.get(i);
				if ((s.getType() == SeriesType.LineSeries || s.getType() == SeriesType.CurveSeries)
						&& !s.isHidden()) {
					if (!s.scaleDirty_) {
						s.scale_ = this.curveTransforms_.get(s).getValue()
								.getM22();
					}
					if (!s.offsetDirty_) {
						int yAxis = s.getYAxis();
						double origin;
						if (this.getOrientation() == Orientation.Horizontal) {
							origin = this.mapToDeviceWithoutTransform(0.0, 0.0,
									yAxis).getX();
						} else {
							origin = this.mapToDeviceWithoutTransform(0.0, 0.0,
									yAxis).getY();
						}
						double dy = this.curveTransforms_.get(s).getValue()
								.getDy();
						double scale = this.curveTransforms_.get(s).getValue()
								.getM22();
						double offset = -dy + origin * (1 - scale)
								+ this.yAxis(yAxis).mapToDevice(0.0, 0);
						if (this.getOrientation() == Orientation.Horizontal) {
							s.offset_ = -this.yAxis(yAxis)
									.mapFromDevice(offset);
						} else {
							s.offset_ = this.yAxis(yAxis).mapFromDevice(offset);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the current zoom range transform.
	 * <p>
	 * This transform maps device coordinates from the fully zoomed out position
	 * to the current zoom range.
	 * <p>
	 * This transform is a
	 * {@link WJavaScriptExposableObject#isJavaScriptBound() JavaScript bound}
	 * transform if this chart is interactive. Otherwise, this transform is just
	 * the identity transform.
	 * <p>
	 * 
	 * @see WCartesianChart#setZoomEnabled(boolean zoomEnabled)
	 * @see WCartesianChart#setPanEnabled(boolean panEnabled)
	 * @see WAxis#setZoomRange(double minimum, double maximum)
	 */
	protected WTransform zoomRangeTransform(int yAxis) {
		return this.zoomRangeTransform(this.xAxis_.transform,
				this.yAxes_.get(yAxis).transform);
	}

	private List<WAxis> collectYAxesAtLocation(AxisValue side) {
		List<WAxis> result = new ArrayList<WAxis>();
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			final WAxis axis = this.yAxes_.get(i).axis;
			if (!axis.isVisible()) {
				continue;
			}
			if (axis.getLocation() == AxisValue.ZeroValue) {
				if (side == AxisValue.MinimumValue) {
					if (this.xAxis_.axis.segments_.get(0).renderMinimum >= 0
							|| !this.xAxis_.axis.isOnAxis(0.0)
							&& this.xAxis_.axis.segments_
									.get(this.xAxis_.axis.segments_.size() - 1).renderMaximum > 0) {
						result.add(axis);
					}
				} else {
					if (side == AxisValue.MaximumValue) {
						if (this.xAxis_.axis.segments_
								.get(this.xAxis_.axis.segments_.size() - 1).renderMaximum <= 0) {
							result.add(axis);
						}
					}
				}
			}
		}
		for (int i = 0; i < this.yAxes_.size(); ++i) {
			final WAxis axis = this.yAxes_.get(i).axis;
			if (!axis.isVisible()) {
				continue;
			}
			if (axis.getLocation() == side
					|| axis.getLocation() == AxisValue.BothSides) {
				result.add(axis);
			}
		}
		return result;
	}

	private int getSeriesIndexOf(int modelColumn) {
		for (int i = 0; i < this.series_.size(); ++i) {
			if (this.series_.get(i).getModelColumn() == modelColumn) {
				return i;
			}
		}
		return -1;
	}

	int getSeriesIndexOf(final WDataSeries series) {
		for (int i = 0; i < this.series_.size(); ++i) {
			if (this.series_.get(i) == series) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasInwardsYAxisOnMaximumSide() {
		List<WAxis> maximumYaxes = this
				.collectYAxesAtLocation(AxisValue.MaximumValue);
		return !maximumYaxes.isEmpty()
				&& maximumYaxes.get(0).getTickDirection() == TickDirection.Inwards;
	}

	private void clearPens() {
		this.clearPensForAxis(Axis.XAxis, -1);
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			this.clearPensForAxis(Axis.YAxis, i);
		}
	}

	private void clearPensForAxis(Axis ax, int yAxis) {
		final List<WCartesianChart.PenAssignment> assignments = ax == Axis.XAxis ? this.xAxis_.pens
				: this.yAxes_.get(yAxis).pens;
		for (int i = 0; i < assignments.size(); ++i) {
			final WCartesianChart.PenAssignment assignment = assignments.get(i);
			this.freePens_.add(assignment.pen);
			this.freePens_.add(assignment.textPen);
			this.freePens_.add(assignment.gridPen);
		}
		assignments.clear();
	}

	private void createPensForAxis(Axis ax, int yAxis) {
		if (!this.getAxis(ax).isVisible()
				|| this.getAxis(ax).getScale() == AxisScale.LogScale) {
			return;
		}
		final WAxis axis = ax == Axis.XAxis ? this.getAxis(Axis.XAxis) : this
				.yAxis(yAxis);
		final WCartesianChart.AxisStruct axisStruct = ax == Axis.XAxis ? this.xAxis_
				: this.yAxes_.get(yAxis);
		double zoom = axis.getZoom();
		if (zoom > axis.getMaxZoom()) {
			zoom = axis.getMaxZoom();
		}
		int level = toZoomLevel(zoom);
		List<WCartesianChart.PenAssignment> assignments = new ArrayList<WCartesianChart.PenAssignment>();
		for (int i = 1;; ++i) {
			double z = Math.pow(2.0, i - 1);
			if (z > axis.getMaxZoom()) {
				break;
			}
			WJavaScriptHandle<WPen> pen = null;
			if (this.freePens_.size() > 0) {
				pen = this.freePens_.get(this.freePens_.size() - 1);
				this.freePens_.remove(this.freePens_.size() - 1);
			} else {
				pen = this.createJSPen();
			}
			WPen p = axis.getPen().clone();
			p.setColor(new WColor(p.getColor().getRed(), p.getColor()
					.getGreen(), p.getColor().getBlue(), i == level ? p
					.getColor().getAlpha() : 0));
			pen.setValue(p);
			WJavaScriptHandle<WPen> textPen = null;
			if (this.freePens_.size() > 0) {
				textPen = this.freePens_.get(this.freePens_.size() - 1);
				this.freePens_.remove(this.freePens_.size() - 1);
			} else {
				textPen = this.createJSPen();
			}
			p = axis.getTextPen().clone();
			p.setColor(new WColor(p.getColor().getRed(), p.getColor()
					.getGreen(), p.getColor().getBlue(), i == level ? p
					.getColor().getAlpha() : 0));
			textPen.setValue(p);
			WJavaScriptHandle<WPen> gridPen = null;
			if (this.freePens_.size() > 0) {
				gridPen = this.freePens_.get(this.freePens_.size() - 1);
				this.freePens_.remove(this.freePens_.size() - 1);
			} else {
				gridPen = this.createJSPen();
			}
			p = axis.getGridLinesPen().clone();
			p.setColor(new WColor(p.getColor().getRed(), p.getColor()
					.getGreen(), p.getColor().getBlue(), i == level ? p
					.getColor().getAlpha() : 0));
			gridPen.setValue(p);
			assignments.add(new WCartesianChart.PenAssignment(pen, textPen,
					gridPen));
		}
		Utils.copyList(assignments, axisStruct.pens);
	}

	String getCObjJsRef() {
		return "jQuery.data(" + this.getJsRef() + ",'cobj')";
	}

	private void assignJSHandlesForAllSeries() {
		if (!this.isInteractive()) {
			return;
		}
		for (int i = 0; i < this.series_.size(); ++i) {
			final WDataSeries s = this.series_.get(i);
			if (s.getType() == SeriesType.LineSeries
					|| s.getType() == SeriesType.CurveSeries) {
				this.assignJSPathsForSeries(s);
				this.assignJSTransformsForSeries(s);
			}
		}
	}

	private void freeJSHandlesForAllSeries() {
		this.freeAllJSPaths();
		this.freeAllJSTransforms();
	}

	private void assignJSPathsForSeries(final WDataSeries series) {
		if (!this.isInteractive()) {
			return;
		}
		WJavaScriptHandle<WPainterPath> handle = null;
		if (this.freePainterPaths_.size() > 0) {
			handle = this.freePainterPaths_
					.get(this.freePainterPaths_.size() - 1);
			this.freePainterPaths_.remove(this.freePainterPaths_.size() - 1);
		} else {
			handle = this.createJSPainterPath();
		}
		this.curvePaths_.put(series, handle);
	}

	private void freeJSPathsForSeries(final WDataSeries series) {
		this.freePainterPaths_.add(this.curvePaths_.get(series));
		this.curvePaths_.remove(series);
	}

	private void freeAllJSPaths() {
		for (Iterator<Map.Entry<WDataSeries, WJavaScriptHandle<WPainterPath>>> it_it = this.curvePaths_
				.entrySet().iterator(); it_it.hasNext();) {
			Map.Entry<WDataSeries, WJavaScriptHandle<WPainterPath>> it = it_it
					.next();
			this.freePainterPaths_.add(it.getValue());
		}
		this.curvePaths_.clear();
	}

	private void assignJSTransformsForSeries(final WDataSeries series) {
		if (!this.isInteractive()) {
			return;
		}
		WJavaScriptHandle<WTransform> handle = null;
		if (this.freeTransforms_.size() > 0) {
			handle = this.freeTransforms_.get(this.freeTransforms_.size() - 1);
			this.freeTransforms_.remove(this.freeTransforms_.size() - 1);
		} else {
			handle = this.createJSTransform();
		}
		this.curveTransforms_.put(series, handle);
	}

	private void freeJSTransformsForSeries(final WDataSeries series) {
		this.freeTransforms_.add(this.curveTransforms_.get(series));
		this.curveTransforms_.remove(series);
	}

	private void freeAllJSTransforms() {
		for (Iterator<Map.Entry<WDataSeries, WJavaScriptHandle<WTransform>>> it_it = this.curveTransforms_
				.entrySet().iterator(); it_it.hasNext();) {
			Map.Entry<WDataSeries, WJavaScriptHandle<WTransform>> it = it_it
					.next();
			this.freeTransforms_.add(it.getValue());
		}
		this.curveTransforms_.clear();
	}

	private void updateJSPens(final StringBuilder js) {
		js.append("pens:{x:");
		this.updateJSPensForAxis(js, Axis.XAxis, -1);
		js.append(",y:[");
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			if (i != 0) {
				js.append(',');
			}
			this.updateJSPensForAxis(js, Axis.YAxis, i);
		}
		js.append("]},");
		js.append("penAlpha:{x:[");
		js.append(this.getAxis(Axis.XAxis).getPen().getColor().getAlpha())
				.append(',');
		js.append(this.getAxis(Axis.XAxis).getTextPen().getColor().getAlpha())
				.append(',');
		js.append(this.getAxis(Axis.XAxis).getGridLinesPen().getColor()
				.getAlpha());
		js.append("],y:[");
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			if (i != 0) {
				js.append(',');
			}
			js.append('[').append(this.yAxis(i).getPen().getColor().getAlpha())
					.append(',');
			js.append(this.yAxis(i).getTextPen().getColor().getAlpha()).append(
					',');
			js.append(this.yAxis(i).getGridLinesPen().getColor().getAlpha())
					.append(']');
		}
		js.append("]},");
	}

	private void updateJSPensForAxis(final StringBuilder js, Axis axis,
			int yAxis) {
		final List<WCartesianChart.PenAssignment> pens = axis == Axis.XAxis ? this.xAxis_.pens
				: this.yAxes_.get(yAxis).pens;
		js.append("[");
		for (int i = 0; i < pens.size(); ++i) {
			if (i != 0) {
				js.append(",");
			}
			final WCartesianChart.PenAssignment assignment = pens.get(i);
			js.append("[");
			js.append(assignment.pen.getJsRef());
			js.append(",");
			js.append(assignment.textPen.getJsRef());
			js.append(",");
			js.append(assignment.gridPen.getJsRef());
			js.append("]");
		}
		js.append("]");
	}

	private void updateJSConfig(final String key, Object value) {
		if (this.getMethod() == WPaintedWidget.Method.HtmlCanvas) {
			if (!this.cObjCreated_) {
				this.update();
			} else {
				this.doJavaScript(this.getCObjJsRef() + ".updateConfig({" + key
						+ ":" + StringUtils.asString(value).toString() + "});");
			}
		}
	}

	WPainterPath pathForSeries(final WDataSeries series) {
		WJavaScriptHandle<WPainterPath> it = this.curvePaths_.get(series);
		if (it == null) {
			return new WPainterPath();
		} else {
			return it.getValue();
		}
	}

	private WTransform zoomRangeTransform(final WTransform xTransform,
			final WTransform yTransform) {
		if (this.getOrientation() == Orientation.Vertical) {
			return new WTransform(1, 0, 0, -1, this.chartArea_.getLeft(),
					this.chartArea_.getBottom())
					.multiply(xTransform)
					.multiply(yTransform)
					.multiply(
							new WTransform(1, 0, 0, -1, -this.chartArea_
									.getLeft(), this.chartArea_.getBottom()));
		} else {
			WRectF area = this.hv(this.chartArea_);
			return new WTransform(0, 1, 1, 0, area.getLeft(), area.getTop())
					.multiply(xTransform)
					.multiply(yTransform)
					.multiply(
							new WTransform(0, 1, 1, 0, -area.getTop(), -area
									.getLeft()));
		}
	}

	WTransform calculateCurveTransform(final WDataSeries series) {
		int yAxis = series.getYAxis();
		double origin;
		if (this.getOrientation() == Orientation.Horizontal) {
			origin = this.mapToDeviceWithoutTransform(0.0, 0.0, yAxis).getX();
		} else {
			origin = this.mapToDeviceWithoutTransform(0.0, 0.0, yAxis).getY();
		}
		double offset = this.yAxis(yAxis).mapToDevice(0.0, 0)
				- this.yAxis(yAxis).mapToDevice(series.getOffset(), 0);
		if (this.getOrientation() == Orientation.Horizontal) {
			offset = -offset;
		}
		return new WTransform(1, 0, 0, series.getScale(), 0, origin
				* (1 - series.getScale()) + offset);
	}

	WTransform curveTransform(final WDataSeries series) {
		WJavaScriptHandle<WTransform> it = this.curveTransforms_.get(series);
		WTransform t = new WTransform();
		if (it == null) {
			t.assign(this.calculateCurveTransform(series));
		} else {
			t.assign(it.getValue());
		}
		if (this.getOrientation() == Orientation.Vertical) {
			return t;
		} else {
			return new WTransform(0, 1, 1, 0, 0, 0).multiply(t).multiply(
					new WTransform(0, 1, 1, 0, 0, 0));
		}
	}

	private void setZoomAndPan() {
		WTransform xTransform = new WTransform();
		if (this.getAxis(Axis.XAxis).zoomMin_ != WAxis.AUTO_MINIMUM
				|| this.getAxis(Axis.XAxis).zoomMax_ != WAxis.AUTO_MAXIMUM) {
			double xPan = -this.getAxis(Axis.XAxis).mapToDevice(
					this.getAxis(Axis.XAxis).getPan(), 0);
			double xZoom = this.getAxis(Axis.XAxis).getZoom();
			if (xZoom > this.getAxis(Axis.XAxis).getMaxZoom()) {
				xZoom = this.getAxis(Axis.XAxis).getMaxZoom();
			}
			xTransform.assign(new WTransform(xZoom, 0, 0, 1, xZoom * xPan, 0));
		}
		List<WTransform> yTransforms = new ArrayList<WTransform>();
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			if (this.yAxis(i).zoomMin_ != WAxis.AUTO_MINIMUM
					|| this.yAxis(i).zoomMax_ != WAxis.AUTO_MAXIMUM) {
				double yPan = -this.yAxis(i).mapToDevice(
						this.yAxis(i).getPan(), 0);
				double yZoom = this.yAxis(i).getZoom();
				if (yZoom > this.yAxis(i).getMaxZoom()) {
					yZoom = this.yAxis(i).getMaxZoom();
				}
				yTransforms
						.add(new WTransform(1, 0, 0, yZoom, 0, yZoom * yPan));
			} else {
				yTransforms.add(new WTransform());
			}
		}
		WRectF chartArea = this.hv(this.getInsideChartArea());
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			WRectF transformedArea = this.zoomRangeTransform(xTransform,
					yTransforms.get(i)).map(chartArea);
			if (transformedArea.getLeft() > chartArea.getLeft()) {
				double diff = chartArea.getLeft() - transformedArea.getLeft();
				if (this.getOrientation() == Orientation.Vertical) {
					xTransform.assign(new WTransform(1, 0, 0, 1, diff, 0)
							.multiply(xTransform));
				} else {
					yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, diff)
							.multiply(yTransforms.get(i)));
				}
				transformedArea = this.zoomRangeTransform(xTransform,
						yTransforms.get(i)).map(chartArea);
			}
			if (transformedArea.getRight() < chartArea.getRight()) {
				double diff = chartArea.getRight() - transformedArea.getRight();
				if (this.getOrientation() == Orientation.Vertical) {
					xTransform.assign(new WTransform(1, 0, 0, 1, diff, 0)
							.multiply(xTransform));
				} else {
					yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, diff)
							.multiply(yTransforms.get(i)));
				}
				transformedArea = this.zoomRangeTransform(xTransform,
						yTransforms.get(i)).map(chartArea);
			}
			if (transformedArea.getTop() > chartArea.getTop()) {
				double diff = chartArea.getTop() - transformedArea.getTop();
				if (this.getOrientation() == Orientation.Vertical) {
					yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, -diff)
							.multiply(yTransforms.get(i)));
				} else {
					xTransform.assign(new WTransform(1, 0, 0, 1, diff, 0)
							.multiply(xTransform));
				}
				transformedArea = this.zoomRangeTransform(xTransform,
						yTransforms.get(i)).map(chartArea);
			}
			if (transformedArea.getBottom() < chartArea.getBottom()) {
				double diff = chartArea.getBottom()
						- transformedArea.getBottom();
				if (this.getOrientation() == Orientation.Vertical) {
					yTransforms.set(i, new WTransform(1, 0, 0, 1, 0, -diff)
							.multiply(yTransforms.get(i)));
				} else {
					xTransform.assign(new WTransform(1, 0, 0, 1, diff, 0)
							.multiply(xTransform));
				}
				transformedArea = this.zoomRangeTransform(xTransform,
						yTransforms.get(i)).map(chartArea);
			}
		}
		this.xAxis_.transformHandle.setValue(xTransform);
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			this.yAxes_.get(i).transformHandle.setValue(yTransforms.get(i));
		}
		this.getAxis(Axis.XAxis).zoomRangeDirty_ = false;
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			this.yAxis(i).zoomRangeDirty_ = false;
		}
	}

	private void addAreaMask() {
		WRectF all = this.hv(new WRectF(0, 0, this.width_, this.height_));
		WRectF chart = this.hv(this.chartArea_);
		List<WRectF> rects = new ArrayList<WRectF>();
		rects.add(new WRectF(all.getTopLeft(), new WPointF(all.getRight(),
				chart.getTop())));
		rects.add(new WRectF(new WPointF(all.getLeft(), chart.getBottom()), all
				.getBottomRight()));
		rects.add(new WRectF(new WPointF(all.getLeft(), chart.getTop()), chart
				.getBottomLeft()));
		rects.add(new WRectF(chart.getTopRight(), new WPointF(all.getRight(),
				chart.getBottom())));
		for (int i = 0; i < rects.size(); ++i) {
			if (rects.get(i).getHeight() > 0 && rects.get(i).getWidth() > 0) {
				WRectArea rect = new WRectArea(rects.get(i));
				rect.setHole(true);
				rect.setTransformable(false);
				this.addArea(rect);
			}
		}
	}

	private void xTransformChanged() {
		this.getAxis(Axis.XAxis)
				.zoomRangeChanged()
				.trigger(this.getAxis(Axis.XAxis).getZoomMinimum(),
						this.getAxis(Axis.XAxis).getZoomMaximum());
	}

	private void yTransformChanged(int yAxis) {
		this.yAxis(yAxis)
				.zoomRangeChanged()
				.trigger(this.yAxis(yAxis).getZoomMinimum(),
						this.yAxis(yAxis).getZoomMaximum());
	}

	private void jsSeriesSelected(double x, double y) {
		if (!this.isSeriesSelectionEnabled()) {
			return;
		}
		double smallestSqDistance = Double.POSITIVE_INFINITY;
		WDataSeries closestSeries = null;
		WPointF closestPointPx = new WPointF();
		WPointF closestPointBeforeSeriesTransform = new WPointF();
		for (int i = 0; i < this.series_.size(); ++i) {
			final WDataSeries series = this.series_.get(i);
			if (!series.isHidden()
					&& (series.getType() == SeriesType.LineSeries || series
							.getType() == SeriesType.CurveSeries)) {
				WTransform transform = this.zoomRangeTransform(
						this.xAxis_.transformHandle.getValue(), this.yAxes_
								.get(series.getYAxis()).transformHandle
								.getValue());
				WPointF p = transform.getInverted().map(new WPointF(x, y));
				WPainterPath path = this.pathForSeries(series);
				WTransform t = this.curveTransform(series);
				for (int j = 0; j < path.getSegments().size(); ++j) {
					final WPainterPath.Segment seg = path.getSegments().get(j);
					if (seg.getType() != WPainterPath.Segment.Type.CubicC1
							&& seg.getType() != WPainterPath.Segment.Type.CubicC2
							&& seg.getType() != WPainterPath.Segment.Type.QuadC) {
						WPointF segP = t
								.map(new WPointF(seg.getX(), seg.getY()));
						double dx = p.getX() - segP.getX();
						double dy = p.getY() - segP.getY();
						double d2 = dx * dx + dy * dy;
						if (d2 < smallestSqDistance) {
							smallestSqDistance = d2;
							closestSeries = series;
							closestPointPx = segP;
							closestPointBeforeSeriesTransform = new WPointF(
									seg.getX(), seg.getY());
						}
					}
				}
			}
		}
		{
			WTransform transform = this
					.zoomRangeTransform(this.xAxis_.transformHandle.getValue(),
							this.yAxes_
									.get(closestSeries != null ? closestSeries
											.getYAxis() : 0).transformHandle
									.getValue());
			WPointF closestDisplayPoint = transform.map(closestPointPx);
			double dx = closestDisplayPoint.getX() - x;
			double dy = closestDisplayPoint.getY() - y;
			double d2 = dx * dx + dy * dy;
			if (d2 > CURVE_SELECTION_DISTANCE_SQUARED) {
				return;
			}
		}
		this.setSelectedSeries(closestSeries);
		if (closestSeries != null) {
			this.seriesSelected_.trigger(closestSeries, this
					.mapFromDeviceWithoutTransform(
							closestPointBeforeSeriesTransform,
							closestSeries.getAxis()));
		} else {
			this.seriesSelected_.trigger((WDataSeries) null, this
					.mapFromDeviceWithoutTransform(
							closestPointBeforeSeriesTransform, Axis.YAxis));
		}
	}

	private void loadTooltip(double x, double y) {
		double px = this
				.zoomRangeTransform(this.xAxis_.transformHandle.getValue(),
						new WTransform()).getInverted()
				.map(new WPointF(x, 0.0)).getX();
		double rx = MarkerMatchIterator.MATCH_RADIUS
				/ this.xAxis_.transformHandle.getValue().getM11();
		List<Double> pys = new ArrayList<Double>();
		List<Double> rys = new ArrayList<Double>();
		for (int i = 0; i < this.getYAxisCount(); ++i) {
			WPointF p = this
					.zoomRangeTransform(new WTransform(),
							this.yAxes_.get(i).transformHandle.getValue())
					.getInverted().map(new WPointF(0.0, y));
			pys.add(p.getY());
			rys.add(MarkerMatchIterator.MATCH_RADIUS
					/ this.yAxes_.get(i).transformHandle.getValue().getM22());
		}
		MarkerMatchIterator iterator = new MarkerMatchIterator(this, px, pys,
				rx, rys);
		this.iterateSeries(iterator, (WPainter) null);
		if (iterator.getMatchedSeries() != null) {
			final WDataSeries series = iterator.getMatchedSeries();
			WString tooltip = series.getModel().getToolTip(iterator.getYRow(),
					iterator.getYColumn());
			boolean isDeferred = !EnumUtils.mask(
					series.getModel().flags(iterator.getYRow(),
							iterator.getYColumn()),
					ItemFlag.ItemHasDeferredTooltip).isEmpty();
			boolean isXHTML = !EnumUtils.mask(
					series.getModel().flags(iterator.getYRow(),
							iterator.getYColumn()), ItemFlag.ItemIsXHTMLText)
					.isEmpty();
			if (!(tooltip.length() == 0) && isDeferred | isXHTML) {
				if (isXHTML) {
					boolean res = removeScript(tooltip);
					if (!res) {
						tooltip = escapeText(tooltip);
					}
				} else {
					tooltip = escapeText(tooltip);
				}
				this.doJavaScript(this.getCObjJsRef() + ".updateTooltip("
						+ WString.toWString(tooltip).getJsStringLiteral()
						+ ");");
			}
		} else {
			for (int btt = 0; btt < this.barTooltips_.size(); ++btt) {
				double[] xs = this.barTooltips_.get(btt).xs;
				double[] ys = this.barTooltips_.get(btt).ys;
				int j = 0;
				int k = 3;
				boolean c = false;
				WPointF p = this
						.zoomRangeTransform(
								this.xAxis_.transformHandle.getValue(),
								this.yAxes_.get(this.barTooltips_.get(btt).series
										.getYAxis()).transformHandle.getValue())
						.getInverted().map(new WPointF(x, y));
				for (; j < 4; k = j++) {
					if ((ys[j] <= p.getY() && p.getY() < ys[k] || ys[k] <= p
							.getY() && p.getY() < ys[j])
							&& p.getX() < (xs[k] - xs[j]) * (p.getY() - ys[j])
									/ (ys[k] - ys[j]) + xs[j]) {
						c = !c;
					}
				}
				if (c) {
					WString tooltip = this.barTooltips_.get(btt).series
							.getModel().getToolTip(
									this.barTooltips_.get(btt).yRow,
									this.barTooltips_.get(btt).yColumn);
					if (!(tooltip.length() == 0)) {
						this.doJavaScript(this.getCObjJsRef()
								+ ".updateTooltip("
								+ WString.toWString(escapeText(tooltip, false))
										.getJsStringLiteral() + ");");
					}
					return;
				}
			}
		}
	}

	WPointF hv(double x, double y, double width) {
		if (this.orientation_ == Orientation.Vertical) {
			return new WPointF(x, y);
		} else {
			return new WPointF(width - y, x);
		}
	}

	private WPointF inverseHv(double x, double y, double width) {
		if (this.orientation_ == Orientation.Vertical) {
			return new WPointF(x, y);
		} else {
			return new WPointF(y, width - x);
		}
	}

	WPointF inverseHv(final WPointF p) {
		if (p.isJavaScriptBound()) {
			if (this.getOrientation() == Orientation.Vertical) {
				return p;
			} else {
				return p.inverseSwapHV(this.height_);
			}
		}
		return this.inverseHv(p.getX(), p.getY(), this.height_);
	}

	private WRectF getInsideChartArea() {
		final WAxis xAxis = this.getAxis(Axis.XAxis);
		final WAxis.Segment xs = xAxis.segments_.get(0);
		double xRenderStart = xAxis.isInverted() ? xAxis.mapToDevice(
				xs.renderMaximum, 0) : xs.renderStart;
		double xRenderEnd = xAxis.isInverted() ? xAxis.mapToDevice(
				xs.renderMinimum, 0) : xs.renderStart + xs.renderLength;
		double yRenderStart = 0;
		double yRenderEnd = 0;
		if (this.getYAxisCount() >= 1) {
			final WAxis yAxis = this.getAxis(Axis.YAxis);
			final WAxis.Segment ys = yAxis.segments_.get(0);
			yRenderStart = yAxis.isInverted() ? yAxis.mapToDevice(
					ys.renderMaximum, 0) : ys.renderStart;
			yRenderEnd = yAxis.isInverted() ? yAxis.mapToDevice(
					ys.renderMinimum, 0) : ys.renderStart + ys.renderLength;
		}
		double x1 = this.chartArea_.getLeft() + xRenderStart;
		double x2 = this.chartArea_.getLeft() + xRenderEnd;
		double y1 = this.chartArea_.getBottom() - yRenderEnd;
		double y2 = this.chartArea_.getBottom() - yRenderStart;
		return new WRectF(x1, y1, x2 - x1, y2 - y1);
	}

	private int calcYAxisSize(final WAxis axis, WPaintDevice device) {
		if (!EnumUtils.mask(device.getFeatures(),
				WPaintDevice.FeatureFlag.HasFontMetrics).isEmpty()) {
			if (this.getOrientation() == Orientation.Horizontal) {
				WMeasurePaintDevice md = new WMeasurePaintDevice(device);
				double h = TICK_LENGTH;
				h += axis.calcMaxTickLabelSize(md, Orientation.Vertical);
				if (!(axis.getTitle().length() == 0)) {
					h += axis.calcTitleSize(md, Orientation.Vertical);
				}
				return (int) Math.ceil(h);
			} else {
				WMeasurePaintDevice md = new WMeasurePaintDevice(device);
				double w = TICK_LENGTH;
				w += axis.calcMaxTickLabelSize(md, Orientation.Horizontal);
				if (!(axis.getTitle().length() == 0)
						&& axis.getTitleOrientation() == Orientation.Vertical) {
					w += axis.calcTitleSize(md, Orientation.Vertical) + 10;
				}
				return (int) Math.ceil(w);
			}
		} else {
			if (this.getOrientation() == Orientation.Horizontal) {
				return TICK_LENGTH + 20
						+ ((axis.getTitle().length() == 0) ? 0 : 20);
			} else {
				return TICK_LENGTH + 30
						+ ((axis.getTitle().length() == 0) ? 0 : 15);
			}
		}
	}

	private void defineJavaScript() {
		WApplication app = WApplication.getInstance();
		if (app != null && (this.isInteractive() || this.hasDeferredToolTips_)) {
			app.loadJavaScript("js/ChartCommon.js", wtjs2());
			app.doJavaScript(
					"if (!Wt3_3_10.chartCommon) {Wt3_3_10.chartCommon = new "
							+ "Wt3_3_10.ChartCommon("
							+ app.getJavaScriptClass() + "); }", false);
			app.loadJavaScript("js/WCartesianChart.js", wtjs1());
			this.jsDefined_ = true;
		} else {
			this.jsDefined_ = false;
		}
	}

	private boolean axisSliderWidgetForSeries(WDataSeries series) {
		for (int i = 0; i < this.axisSliderWidgets_.size(); ++i) {
			if (this.axisSliderWidgets_.get(i).getSeries() == series) {
				return true;
			}
		}
		return false;
	}

	static class IconWidget extends WPaintedWidget {
		private static Logger logger = LoggerFactory
				.getLogger(IconWidget.class);

		public IconWidget(WCartesianChart chart, int index,
				WContainerWidget parent) {
			super(parent);
			this.chart_ = chart;
			this.index_ = index;
			this.setInline(true);
			this.resize(new WLength(20), new WLength(20));
		}

		public IconWidget(WCartesianChart chart, int index) {
			this(chart, index, (WContainerWidget) null);
		}

		protected void paintEvent(WPaintDevice paintDevice) {
			WPainter painter = new WPainter(paintDevice);
			this.chart_.renderLegendIcon(painter, new WPointF(2.5, 10.0),
					this.chart_.getSeries(this.index_));
		}

		private WCartesianChart chart_;
		private int index_;
	}

	static WJavaScriptPreamble wtjs2() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptConstructor,
				"ChartCommon",
				"function(x){function E(a,b,c,d){function e(j){return c?b[j]:b[n-1-j]}function i(j){for(;e(j)[2]===v||e(j)[2]===y;)j--;return j}var k=l;if(d)k=m;var n=b.length;d=Math.floor(n/2);d=i(d);var q=0,s=n,f=false;if(e(0)[k]>a)return c?-1:n;if(e(n-1)[k]<a)return c?n:-1;for(;!f;){var h=d+1;if(h<n&&(e(h)[2]===v||e(h)[2]===y))h+=2;if(e(d)[k]>a){s=d;d=Math.floor((s+q)/2);d=i(d)}else if(e(d)[k]===a)f=true;else if(h<n&&e(h)[k]>a)f=true;else if(h<n&&e(h)[k]=== a){d=h;f=true}else{q=d;d=Math.floor((s+q)/2);d=i(d)}}return c?d:n-1-d}function G(a,b){return b[0][a]<b[b.length-1][a]}var v=2,y=3,l=0,m=1,F=this;x=x.WT.gfxUtils;var A=x.rect_top,B=x.rect_bottom,z=x.rect_left,C=x.rect_right,H=x.transform_mult;this.findClosestPoint=function(a,b,c){var d=l;if(c)d=m;var e=G(d,b);c=E(a,b,e,c);if(c<0)c=0;if(c>=b.length)return[b[b.length-1][l],b[b.length-1][m]];if(c>=b.length)c=b.length-2;if(b[c][d]===a)return[b[c][l],b[c][m]];var i=e?c+1:c-1;if(e&&b[i][2]==v)i+=2;if(!e&& i<0)return[b[c][l],b[c][m]];if(!e&&i>0&&b[i][2]==y)i-=2;e=Math.abs(a-b[c][d]);a=Math.abs(b[i][d]-a);return e<a?[b[c][l],b[c][m]]:[b[i][l],b[i][m]]};this.minMaxY=function(a,b){b=b?l:m;for(var c=a[0][b],d=a[0][b],e=1;e<a.length;++e)if(a[e][2]!==v&&a[e][2]!==y&&a[e][2]!==5){if(a[e][b]>d)d=a[e][b];if(a[e][b]<c)c=a[e][b]}return[c,d]};this.projection=function(a,b){var c=Math.cos(a);a=Math.sin(a);var d=c*a,e=-b[0]*c-b[1]*a;return[c*c,d,d,a*a,c*e+b[0],a*e+b[1]]};this.distanceSquared=function(a,b){a=[b[l]- a[l],b[m]-a[m]];return a[l]*a[l]+a[m]*a[m]};this.distanceLessThanRadius=function(a,b,c){return c*c>=F.distanceSquared(a,b)};this.toZoomLevel=function(a){return Math.floor(Math.log(a)/Math.LN2+0.5)+1};this.isPointInRect=function(a,b){var c;if(a.x!==undefined){c=a.x;a=a.y}else{c=a[0];a=a[1]}return c>=z(b)&&c<=C(b)&&a>=A(b)&&a<=B(b)};this.toDisplayCoord=function(a,b,c,d,e){if(c){a=[(a[l]-e[0])/e[2],(a[m]-e[1])/e[3]];d=[d[0]+a[m]*d[2],d[1]+a[l]*d[3]]}else{a=[(a[l]-e[0])/e[2],1-(a[m]-e[1])/e[3]];d=[d[0]+ a[l]*d[2],d[1]+a[m]*d[3]]}return H(b,d)};this.findYRange=function(a,b,c,d,e,i,k,n){if(a.length!==0){var q=F.toDisplayCoord([c,0],[1,0,0,1,0,0],e,i,k),s=F.toDisplayCoord([d,0],[1,0,0,1,0,0],e,i,k),f=e?m:l,h=e?l:m,j=G(f,a),g=E(q[f],a,j,e),o=E(s[f],a,j,e),p,r,t=Infinity,u=-Infinity,D=g===o&&g===a.length||g===-1&&o===-1;if(!D){if(j)if(g<0)g=0;else{g++;if(a[g]&&a[g][2]===v)g+=2}else if(g>=a.length-1)g=a.length-2;if(!j&&o<0)o=0;for(p=Math.min(g,o);p<=Math.max(g,o)&&p<a.length;++p)if(a[p][2]!==v&&a[p][2]!== y){if(a[p][h]<t)t=a[p][h];if(a[p][h]>u)u=a[p][h]}if(j&&g>0||!j&&g<a.length-1){if(j){r=g-1;if(a[r]&&a[r][2]===y)r-=2}else{r=g+1;if(a[r]&&a[r][2]===v)r+=2}p=(q[f]-a[r][f])/(a[g][f]-a[r][f]);g=a[r][h]+p*(a[g][h]-a[r][h]);if(g<t)t=g;if(g>u)u=g}if(j&&o<a.length-1||!j&&o>0){if(j){j=o+1;if(a[j][2]===v)j+=2}else{j=o-1;if(a[j][2]===y)j-=2}p=(s[f]-a[o][f])/(a[j][f]-a[o][f]);g=a[o][h]+p*(a[j][h]-a[o][h]);if(g<t)t=g;if(g>u)u=g}}var w;a=k[2]/(d-c);c=e?2:3;if(!D){w=i[c]/(u-t);w=i[c]/(i[c]/w+20);if(w>n.y[b])w=n.y[b]}b= e?[q[m]-A(i),!D?(t+u)/2-i[2]/w/2-z(i):0]:[q[l]-z(i),!D?-((t+u)/2+i[3]/w/2-B(i)):0];return{xZoom:a,yZoom:w,panPoint:b}}};this.matchAxis=function(a,b,c,d,e){function i(){return d.length}function k(h){return d[h].side}function n(h){return d[h].width}function q(h){return d[h].minOffset}function s(h){return d[h].maxOffset}if(e){if(a<z(c)||a>C(c))return-1}else if(b<A(c)||b>B(c))return-1;for(var f=0;f<i();++f)if(e)if((k(f)===\"min\"||k(f)===\"both\")&&b>=A(c)-q(f)-n(f)&&b<=A(c)-q(f))return f;else{if((k(f)=== \"max\"||k(f)===\"both\")&&b>=B(c)+s(f)&&b<=B(c)+s(f)+n(f))return f}else if((k(f)===\"min\"||k(f)===\"both\")&&a>=z(c)-q(f)-n(f)&&a<=z(c)-q(f))return f;else if((k(f)===\"max\"||k(f)===\"both\")&&a>=C(c)+s(f)&&a<=C(c)+s(f)+n(f))return f;return-1}}");
	}

	static WJavaScriptPreamble wtjs1() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptConstructor,
				"WCartesianChart",
				"function(ta,I,x,k){function M(a){return a===undefined}function m(a){return k.modelAreas[a]}function S(){return k.followCurve}function ua(){return k.crosshair||S()!==-1}function A(){return k.isHorizontal}function n(){return k.xTransform}function h(a){return k.yTransforms[a]}function g(){return k.area}function l(){return k.insideArea}function Z(a){return M(a)?k.series:k.series[a]}function aa(a){return Z(a).transform}function hb(a){return A()? y([0,1,1,0,0,0],y(aa(a),[0,1,1,0,0,0])):aa(a)}function Na(a){return Z(a).curve}function N(a){return Z(a).axis}function ib(){return k.seriesSelection}function jb(){return k.sliders}function kb(){return k.hasToolTips}function lb(){return k.coordinateOverlayPadding}function Ea(){return k.curveManipulation}function va(){return k.maxZoom.x}function T(a){return k.maxZoom.y[a]}function K(){return k.pens}function mb(){return k.penAlpha}function ba(){return k.selectedCurve}function wa(a){a.preventDefault&& a.preventDefault()}function ca(a,b){I.addEventListener(a,b)}function U(a,b){I.removeEventListener(a,b)}function B(a){return a.length}function J(){return B(k.yTransforms)}function xb(){if(k.notifyTransform.x)return true;for(var a=0;a<J();++a)if(k.notifyTransform.y[a])return true;return false}function O(){return k.crosshairAxis}function Ya(a){return a.pointerType===2||a.pointerType===3||a.pointerType===\"pen\"||a.pointerType===\"touch\"}function Oa(){if(o){if(o.tooltipTimeout){clearTimeout(o.tooltipTimeout); o.tooltipTimeout=null}if(!o.overTooltip)if(o.tooltipOuterDiv){document.body.removeChild(o.tooltipOuterDiv);o.tooltipEl=null;o.tooltipOuterDiv=null}}}function Fa(){if(xb()){if(Pa){window.clearTimeout(Pa);Pa=null}Pa=setTimeout(function(){if(k.notifyTransform.x&&!nb(Za,n())){ta.emit(x.widget,\"xTransformChanged\");ha(Za,n())}for(var a=0;a<J();++a)if(k.notifyTransform.y[a]&&!nb(Qa[a],h(a))){ta.emit(x.widget,\"yTransformChanged\"+a);ha(Qa[a],h(a))}},yb)}}function ia(a){var b,c;if(A()){b=p(g());c=t(g());return y([0, 1,1,0,b,c],y(n(),y(h(a),[0,1,1,0,-c,-b])))}else{b=p(g());c=w(g());return y([1,0,0,-1,b,c],y(n(),y(h(a),[1,0,0,-1,-b,c])))}}function F(a){return y(ia(a),l())}function ja(a,b,c){if(M(c))c=false;a=c?a:y(Ga(ia(b)),a);a=A()?[(a[u]-g()[1])/g()[3],(a[v]-g()[0])/g()[2]]:[(a[v]-g()[0])/g()[2],1-(a[u]-g()[1])/g()[3]];return[m(b)[0]+a[v]*m(b)[2],m(b)[1]+a[u]*m(b)[3]]}function Ra(a,b,c){if(M(c))c=false;return da.toDisplayCoord(a,c?[1,0,0,1,0,0]:ia(b),A(),g(),m(b))}function Ha(){var a,b;if(A()){a=(ja([0,t(g())], 0)[0]-m(0)[0])/m(0)[2];b=(ja([0,w(g())],0)[0]-m(0)[0])/m(0)[2]}else{a=(ja([p(g()),0],0)[0]-m(0)[0])/m(0)[2];b=(ja([q(g()),0],0)[0]-m(0)[0])/m(0)[2]}var c;for(c=0;c<B(jb());++c){var e=$(\"#\"+jb()[c]);if(e)(e=e.data(\"sobj\"))&&e.changeRange(a,b)}}function V(){Oa();if(kb()&&o.tooltipPosition)o.tooltipTimeout=setTimeout(function(){ob()},pb);ka&&qb(function(){x.repaint();ua()&&$a()})}function $a(){if(ka){var a=H.getContext(\"2d\");a.clearRect(0,0,H.width,H.height);a.save();a.beginPath();a.moveTo(p(g()),t(g())); a.lineTo(q(g()),t(g()));a.lineTo(q(g()),w(g()));a.lineTo(p(g()),w(g()));a.closePath();a.clip();var b=y(Ga(ia(O())),z),c=z[v],e=z[u];if(S()!==-1){b=zb(A()?b[u]:b[v],Na(S()),A());e=y(ia(N(S())),y(hb(S()),b));c=e[v];e=e[u];z[v]=c;z[u]=e}b=A()?[(b[u]-g()[1])/g()[3],(b[v]-g()[0])/g()[2]]:[(b[v]-g()[0])/g()[2],1-(b[u]-g()[1])/g()[3]];b=S()!==-1?[m(N(S()))[0]+b[v]*m(N(S()))[2],m(N(S()))[1]+b[u]*m(N(S()))[3]]:[m(O())[0]+b[v]*m(O())[2],m(O())[1]+b[u]*m(O())[3]];a.fillStyle=a.strokeStyle=k.crosshairColor;a.font= \"16px sans-serif\";a.textAlign=\"right\";a.textBaseline=\"top\";var d=b[0].toFixed(2);b=b[1].toFixed(2);if(d===\"-0.00\")d=\"0.00\";if(b===\"-0.00\")b=\"0.00\";a.fillText(\"(\"+d+\",\"+b+\")\",q(g())-lb()[0],t(g())+lb()[1]);a.setLineDash&&a.setLineDash([1,2]);a.beginPath();a.moveTo(Math.floor(c)+0.5,Math.floor(t(g()))+0.5);a.lineTo(Math.floor(c)+0.5,Math.floor(w(g()))+0.5);a.moveTo(Math.floor(p(g()))+0.5,Math.floor(e)+0.5);a.lineTo(Math.floor(q(g()))+0.5,Math.floor(e)+0.5);a.stroke();a.restore()}}function Ab(a){return t(a)<= t(l())+Sa&&w(a)>=w(l())-Sa&&p(a)<=p(l())+Sa&&q(a)>=q(l())-Sa}function ea(a){for(var b=0;b<J();++b){var c=F(b);if(A())if(a===xa)a=ya;else if(a===ya)a=xa;if(M(a)||a===xa)if(n()[0]<1){n()[0]=1;c=F(b)}if(M(a)||a===ya)if(h(b)[3]<1){h(b)[3]=1;c=F(b)}if(M(a)||a===xa){if(p(c)>p(l())){c=p(l())-p(c);if(A())h(b)[5]=h(b)[5]+c;else n()[4]=n()[4]+c;c=F(b)}if(q(c)<q(l())){c=q(l())-q(c);if(A())h(b)[5]=h(b)[5]+c;else n()[4]=n()[4]+c;c=F(b)}}if(M(a)||a===ya){if(t(c)>t(l())){c=t(l())-t(c);if(A())n()[4]=n()[4]+c;else h(b)[5]= h(b)[5]-c;c=F(b)}if(w(c)<w(l())){c=w(l())-w(c);if(A())n()[4]=n()[4]+c;else h(b)[5]=h(b)[5]-c;F(b)}}}Fa()}function ob(){ta.emit(x.widget,\"loadTooltip\",o.tooltipPosition[v],o.tooltipPosition[u])}function Bb(){if(ua()&&(M(H)||x.canvas.width!==H.width||x.canvas.height!==H.height)){if(H){H.parentNode.removeChild(H);jQuery.removeData(I,\"oobj\");H=undefined}var a=document.createElement(\"canvas\");a.setAttribute(\"width\",x.canvas.width);a.setAttribute(\"height\",x.canvas.height);a.style.position=\"absolute\";a.style.display= \"block\";a.style.left=\"0\";a.style.top=\"0\";if(window.MSPointerEvent||window.PointerEvent){a.style.msTouchAction=\"none\";a.style.touchAction=\"none\"}x.canvas.parentNode.appendChild(a);H=a;jQuery.data(I,\"oobj\",H)}else if(!M(H)&&!ua()){H.parentNode.removeChild(H);jQuery.removeData(I,\"oobj\");H=undefined}z=[(p(g())+q(g()))/2,(t(g())+w(g()))/2]}function rb(){return H?H:x.canvas}function ab(a,b){if(za){var c=Date.now();if(M(b))b=c-la;var e={x:0,y:0},d;if(r===-1){d=F(0);for(var f=1;f<J();++f)d=Ta(d,F(f))}else d= F(r);f=Cb;if(b>2*Ia){ka=false;var j=Math.floor(b/Ia-1),C;for(C=0;C<j;++C){ab(a,Ia);if(!za){ka=true;V();return}}b-=j*Ia;ka=true}if(i.x===Infinity||i.x===-Infinity)i.x=i.x>0?ma:-ma;if(isFinite(i.x)){i.x/=1+sb*b;d[0]+=i.x*b;if(p(d)>p(l())){i.x+=-f*(p(d)-p(l()))*b;i.x*=0.7}else if(q(d)<q(l())){i.x+=-f*(q(d)-q(l()))*b;i.x*=0.7}if(Math.abs(i.x)<bb)if(p(d)>p(l()))i.x=bb;else if(q(d)<q(l()))i.x=-bb;if(Math.abs(i.x)>ma)i.x=(i.x>0?1:-1)*ma;e.x=i.x*b}if(i.y===Infinity||i.y===-Infinity)i.y=i.y>0?ma:-ma;if(isFinite(i.y)){i.y/= 1+sb*b;d[1]+=i.y*b;if(t(d)>t(l())){i.y+=-f*(t(d)-t(l()))*b;i.y*=0.7}else if(w(d)<w(l())){i.y+=-f*(w(d)-w(l()))*b;i.y*=0.7}if(Math.abs(i.y)<0.001)if(t(d)>t(l()))i.y=0.001;else if(w(d)<w(l()))i.y=-0.001;if(Math.abs(i.y)>ma)i.y=(i.y>0?1:-1)*ma;e.y=i.y*b}if(r===-1){d=F(0);for(f=1;f<J();++f)d=Ta(d,F(f))}else d=F(r);W(e,Aa,r);if(r===-1){a=F(0);for(f=1;f<J();++f)a=Ta(a,F(f))}else a=F(r);if(p(d)>p(l())&&p(a)<=p(l())){i.x=0;W({x:-e.x,y:0},Aa,r);ea(xa)}if(q(d)<q(l())&&q(a)>=q(l())){i.x=0;W({x:-e.x,y:0},Aa, r);ea(xa)}if(t(d)>t(l())&&t(a)<=t(l())){i.y=0;W({x:0,y:-e.y},Aa,r);ea(ya)}if(w(d)<w(l())&&w(a)>=w(l())){i.y=0;W({x:0,y:-e.y},Aa,r);ea(ya)}if(Math.abs(i.x)<tb&&Math.abs(i.y)<tb&&Ab(a)){ea();za=false;D=null;i.x=0;i.y=0;la=null;s=[]}else{la=c;ka&&Ua(ab)}}}function Va(){var a,b,c=ub(n()[0])-1;if(c>=B(K().x))c=B(K().x)-1;for(a=0;a<B(K().x);++a)if(c===a)for(b=0;b<B(K().x[a]);++b)K().x[a][b].color[3]=mb().x[b];else for(b=0;b<B(K().x[a]);++b)K().x[a][b].color[3]=0;for(c=0;c<B(K().y);++c){var e=ub(h(c)[3])- 1;if(e>=B(K().y[c]))e=B(K().y[c])-1;for(a=0;a<B(K().y[c]);++a)if(e===a)for(b=0;b<B(K().y[c][a]);++b)K().y[c][a][b].color[3]=mb().y[c][b];else for(b=0;b<B(K().y[c][a]);++b)K().y[c][a][b].color[3]=0}}function W(a,b,c){if(M(b))b=0;if(M(c))c=-1;var e=ja(z,O());if(A())a={x:a.y,y:-a.x};if(b&Aa){if(c===-1){n()[4]=n()[4]+a.x;for(b=0;b<J();++b)h(b)[5]=h(b)[5]-a.y}else h(c)[5]=h(c)[5]-a.y;Fa()}else if(b&vb){var d;if(c===-1){d=F(0);for(b=1;b<J();++b)d=Ta(d,F(b))}else d=F(c);if(p(d)>p(l())){if(a.x>0)a.x/=1+(p(d)- p(l()))*Wa}else if(q(d)<q(l()))if(a.x<0)a.x/=1+(q(l())-q(d))*Wa;if(t(d)>t(l())){if(a.y>0)a.y/=1+(t(d)-t(l()))*Wa}else if(w(d)<w(l()))if(a.y<0)a.y/=1+(w(l())-w(d))*Wa;if(c===-1){n()[4]=n()[4]+a.x;for(b=0;b<J();++b)h(b)[5]=h(b)[5]-a.y}else h(c)[5]=h(c)[5]-a.y;if(c===-1)z[v]+=a.x;z[u]+=a.y;Fa()}else{if(c===-1){n()[4]=n()[4]+a.x;for(b=0;b<J();++b)h(b)[5]=h(b)[5]-a.y}else h(c)[5]=h(c)[5]-a.y;if(c===-1)z[v]+=a.x;z[u]+=a.y;ea()}a=Ra(e,O());z[v]=a[v];z[u]=a[u];V();Ha()}function Ja(a,b,c,e){if(M(e))e=-1;var d= ja(z,O());a=A()?[a.y-t(g()),a.x-p(g())]:y(Ga([1,0,0,-1,p(g()),w(g())]),[a.x,a.y]);var f=a[0];a=a[1];var j=Math.pow(1.2,A()?c:b);b=Math.pow(1.2,A()?b:c);if(n()[0]*j>va())j=va()/n()[0];if(e===-1){if(j<1||n()[0]!==va())Ba(n(),y([j,0,0,1,f-j*f,0],n()));for(e=0;e<J();++e){c=b;if(h(e)[3]*b>T(e))c=T(e)/h(e)[3];if(c<1||h(e)[3]!==T(e))Ba(h(e),y([1,0,0,c,0,a-c*a],h(e)))}}else{if(h(e)[3]*b>T(e))b=T(e)/h(e)[3];if(b<1||h(e)[3]!=T(e))Ba(h(e),y([1,0,0,b,0,a-b*a],h(e)))}ea();d=Ra(d,O());z[v]=d[v];z[u]=d[u];Va(); V();Ha()}jQuery.data(I,\"cobj\",this);var na=this,E=ta.WT;na.config=k;var G=E.gfxUtils,y=G.transform_mult,Ga=G.transform_inverted,ha=G.transform_assign,nb=G.transform_equal,Db=G.transform_apply,t=G.rect_top,w=G.rect_bottom,p=G.rect_left,q=G.rect_right,Ta=G.rect_intersection,da=E.chartCommon,Eb=da.minMaxY,zb=da.findClosestPoint,Fb=da.projection,wb=da.distanceLessThanRadius,ub=da.toZoomLevel,Ka=da.isPointInRect,Gb=da.findYRange,La=function(a,b){return da.matchAxis(a,b,g(),k.yAxes,A())},Ia=17,Ua=function(){return window.requestAnimationFrame|| window.webkitRequestAnimationFrame||window.mozRequestAnimationFrame||function(a){window.setTimeout(a,Ia)}}(),cb=false,qb=function(a){if(!cb){cb=true;Ua(function(){a();cb=false})}};if(window.MSPointerEvent||window.PointerEvent){I.style.touchAction=\"none\";x.canvas.style.msTouchAction=\"none\";x.canvas.style.touchAction=\"none\"}var Aa=1,vb=2,xa=1,ya=2,v=0,u=1,yb=250,pb=500,sb=0.003,Cb=2.0E-4,Wa=0.07,Sa=3,bb=0.001,ma=1.5,tb=0.02,qa=jQuery.data(I,\"eobj2\");if(!qa){qa={};qa.contextmenuListener=function(a){wa(a); U(\"contextmenu\",qa.contextmenuListener)}}jQuery.data(I,\"eobj2\",qa);var X={},ra=false;if(window.MSPointerEvent||window.PointerEvent)(function(){function a(){ra=B(d)>0}function b(j){if(Ya(j)){wa(j);d.push(j);a();X.start(I,{touches:d.slice(0)})}}function c(j){if(ra)if(Ya(j)){wa(j);var C;for(C=0;C<B(d);++C)if(d[C].pointerId===j.pointerId){d.splice(C,1);break}a();X.end(I,{touches:d.slice(0),changedTouches:[]})}}function e(j){if(Ya(j)){wa(j);var C;for(C=0;C<B(d);++C)if(d[C].pointerId===j.pointerId){d[C]= j;break}a();X.moved(I,{touches:d.slice(0)})}}var d=[],f=jQuery.data(I,\"eobj\");if(f)if(window.PointerEvent){U(\"pointerdown\",f.pointerDown);U(\"pointerup\",f.pointerUp);U(\"pointerout\",f.pointerUp);U(\"pointermove\",f.pointerMove)}else{U(\"MSPointerDown\",f.pointerDown);U(\"MSPointerUp\",f.pointerUp);U(\"MSPointerOut\",f.pointerUp);U(\"MSPointerMove\",f.pointerMove)}jQuery.data(I,\"eobj\",{pointerDown:b,pointerUp:c,pointerMove:e});if(window.PointerEvent){ca(\"pointerdown\",b);ca(\"pointerup\",c);ca(\"pointerout\",c);ca(\"pointermove\", e)}else{ca(\"MSPointerDown\",b);ca(\"MSPointerUp\",c);ca(\"MSPointerOut\",c);ca(\"MSPointerMove\",e)}})();var H=jQuery.data(I,\"oobj\"),z=null,ka=true,D=null,r=-1,s=[],fa=false,oa=false,Q=null,db=null,eb=null,i={x:0,y:0},Y=null,la=null,o=jQuery.data(I,\"tobj\");if(!o){o={overTooltip:false};jQuery.data(I,\"tobj\",o)}var Ca=null,za=false,Pa=null,Za=[0,0,0,0,0,0];ha(Za,n());var Qa=[];for(G=0;G<J();++G){Qa.push([0,0,0,0,0,0]);ha(Qa[G],h(G))}var Ba=function(a,b){ha(a,b);Fa()};x.combinedTransform=ia;this.updateTooltip= function(a){Oa();if(a)if(o.tooltipPosition){o.toolTipEl=document.createElement(\"div\");o.toolTipEl.className=k.ToolTipInnerStyle;o.toolTipEl.innerHTML=a;o.tooltipOuterDiv=document.createElement(\"div\");o.tooltipOuterDiv.className=k.ToolTipOuterStyle;document.body.appendChild(o.tooltipOuterDiv);o.tooltipOuterDiv.appendChild(o.toolTipEl);var b=E.widgetPageCoordinates(x.canvas);a=o.tooltipPosition[v]+b.x;b=o.tooltipPosition[u]+b.y;E.fitToWindow(o.tooltipOuterDiv,a+10,b+10,a-10,b-10);$(o.toolTipEl).mouseenter(function(){o.overTooltip= true});$(o.toolTipEl).mouseleave(function(){o.overTooltip=false})}};this.mouseMove=function(a,b){setTimeout(function(){setTimeout(Oa,200);if(!ra){var c=E.widgetCoordinates(x.canvas,b);if(Ka(c,g())){if(!o.tooltipEl&&kb()){o.tooltipPosition=[c.x,c.y];o.tooltipTimeout=setTimeout(function(){ob()},pb)}if(D===null&&ua()&&ka){z=[c.x,c.y];qb($a)}}}},0)};this.mouseOut=function(){setTimeout(Oa,200)};this.mouseDown=function(a,b){if(!ra){a=E.widgetCoordinates(x.canvas,b);b=La(a.x,a.y);var c=Ka(a,g());if(!(b=== -1&&!c)){D=a;r=b}}};this.mouseUp=function(){if(!ra){D=null;r=-1}};this.mouseDrag=function(a,b){if(!ra)if(D!==null){a=E.widgetCoordinates(x.canvas,b);if(E.buttons===1)if(r===-1&&Ea()&&Z(ba())){b=ba();var c;c=A()?a.x-D.x:a.y-D.y;ha(aa(b),y([1,0,0,1,0,c/h(N(seriesNb))[3]],aa(b)));V()}else k.pan&&W({x:a.x-D.x,y:a.y-D.y},0,r);D=a}};this.clicked=function(a,b){if(!ra)if(D===null)if(ib()){a=E.widgetCoordinates(x.canvas,b);ta.emit(x.widget,\"seriesSelected\",a.x,a.y)}};this.mouseWheel=function(a,b){var c=(b.metaKey<< 3)+(b.altKey<<2)+(b.ctrlKey<<1)+b.shiftKey;a=k.wheelActions[c];if(!M(a)){var e=E.widgetCoordinates(x.canvas,b),d=La(e.x,e.y),f=Ka(e,g());if(!(d===-1&&!f)){var j=E.normalizeWheel(b);if(f&&c===0&&Ea()){c=ba();f=-j.spinY;if(Z(c)){a=hb(c);a=Db(a,Na(c));a=Eb(a,A());a=(a[0]+a[1])/2;E.cancelEvent(b);b=Math.pow(1.2,f);ha(aa(c),y([1,0,0,b,0,a-b*a],aa(c)));V();return}}if((a===4||a===5||a===6)&&k.pan){c=n()[4];f=[];for(e=0;e<J();++e)f.push(h(e)[5]);if(a===6)W({x:-j.pixelX,y:-j.pixelY},0,d);else if(a===5)W({x:0, y:-j.pixelX-j.pixelY},0,d);else a===4&&W({x:-j.pixelX-j.pixelY,y:0},0,d);if(c!==n()[4])for(e=0;e<J();++e)f[e]!==h(e)[5]&&E.cancelEvent(b)}else if(k.zoom){E.cancelEvent(b);f=-j.spinY;if(f===0)f=-j.spinX;if(a===1)Ja(e,0,f,d);else if(a===0)Ja(e,f,0,d);else if(a===2)Ja(e,f,f,d);else if(a===3)j.pixelX!==0?Ja(e,f,0,d):Ja(e,0,f,d)}}}};var Hb=function(){ib()&&ta.emit(x.widget,\"seriesSelected\",D.x,D.y)};X.start=function(a,b,c){fa=B(b.touches)===1;oa=B(b.touches)===2;if(fa){za=false;var e=E.widgetCoordinates(x.canvas, b.touches[0]);a=La(e.x,e.y);var d=Ka(e,g());if(a===-1&&!d)return;Ca=a===-1&&ua()&&wb(z,[e.x,e.y],30)?1:0;la=Date.now();D=e;r=a;if(Ca!==1){if(!c&&d)Y=window.setTimeout(Hb,200);ca(\"contextmenu\",qa.contextmenuListener)}E.capture(null);E.capture(rb())}else if(oa&&(k.zoom||Ea())){if(Y){window.clearTimeout(Y);Y=null}za=false;s=[E.widgetCoordinates(x.canvas,b.touches[0]),E.widgetCoordinates(x.canvas,b.touches[1])].map(function(f){return[f.x,f.y]});a=-1;if(!s.every(function(f){return Ka(f,g())})){a=La(s[0][v], s[0][u]);if(a===-1||La(s[1][v],s[1][u])!==a){oa=null;return}r=a}E.capture(null);E.capture(rb());Q=Math.atan2(s[1][1]-s[0][1],s[1][0]-s[0][0]);db=[(s[0][0]+s[1][0])/2,(s[0][1]+s[1][1])/2];c=Math.abs(Math.sin(Q));e=Math.abs(Math.cos(Q));Q=c<Math.sin(0.125*Math.PI)?0:e<Math.cos(0.375*Math.PI)?Math.PI/2:Math.tan(Q)>0?Math.PI/4:-Math.PI/4;eb=Fb(Q,db);r=a}else return;wa(b)};X.end=function(a,b){if(Y){window.clearTimeout(Y);Y=null}window.setTimeout(function(){U(\"contextmenu\",qa.contextmenuListener)},0);var c= Array.prototype.slice.call(b.touches),e=B(c)===0;e||function(){var d;for(d=0;d<B(b.changedTouches);++d)(function(){for(var f=b.changedTouches[d].identifier,j=0;j<B(c);++j)if(c[j].identifier===f){c.splice(j,1);return}})()}();e=B(c)===0;fa=B(c)===1;oa=B(c)===2;if(e){Xa=null;if(Ca===0&&(isFinite(i.x)||isFinite(i.y))&&k.rubberBand){la=Date.now();za=true;Ua(ab)}else{Ca===1&&na.mouseUp(null,null);c=[];la=eb=db=Q=null}Ca=null}else if(fa||oa)X.start(a,b,true)};var Xa=null,sa=null,fb=null;X.moved=function(a, b){if(fa||oa)if(!(fa&&D==null)){wa(b);sa=E.widgetCoordinates(x.canvas,b.touches[0]);if(B(b.touches)>1)fb=E.widgetCoordinates(x.canvas,b.touches[1]);if(r===-1&&fa&&Y&&!wb([sa.x,sa.y],[D.x,D.y],3)){window.clearTimeout(Y);Y=null}Xa||(Xa=setTimeout(function(){if(r===-1&&fa&&Ea()&&Z(ba())){var c=ba();if(Z(c)){var e=sa,d;d=A()?(e.x-D.x)/h(N(ba()))[3]:(e.y-D.y)/h(N(ba()))[3];aa(c)[5]+=d;D=e;V()}}else if(fa){e=sa;d=Date.now();var f={x:e.x-D.x,y:e.y-D.y};c=d-la;la=d;if(Ca===1){z[v]+=f.x;z[u]+=f.y;ua()&&ka&& Ua($a)}else if(k.pan){i.x=f.x/c;i.y=f.y/c;W(f,k.rubberBand?vb:0,r)}D=e}else if(r===-1&&oa&&Ea()&&Z(ba())){f=A()?v:u;d=[sa,fb].map(function(P){return A()?[P.x,pa]:[Ma,P.y]});c=Math.abs(s[1][f]-s[0][f]);var j=Math.abs(d[1][f]-d[0][f]),C=c>0?j/c:1;if(j===c)C=1;c=ba();if(Z(c)){var pa=y(Ga(ia(N(c))),[0,(s[0][f]+s[1][f])/2])[1],Da=y(Ga(ia(N(c))),[0,(d[0][f]+d[1][f])/2])[1];ha(aa(c),y([1,0,0,C,0,-C*pa+Da],aa(c)));D=e;V();s=d}}else if(oa&&k.zoom){e=ja(z,O());var Ma=(s[0][0]+s[1][0])/2;pa=(s[0][1]+s[1][1])/ 2;d=[sa,fb].map(function(P){return Q===0?[P.x,pa]:Q===Math.PI/2?[Ma,P.y]:y(eb,[P.x,P.y])});f=Math.abs(s[1][0]-s[0][0]);c=Math.abs(d[1][0]-d[0][0]);var ga=f>0?c/f:1;if(c===f||Q===Math.PI/2)ga=1;var gb=(d[0][0]+d[1][0])/2;c=Math.abs(s[1][1]-s[0][1]);j=Math.abs(d[1][1]-d[0][1]);C=c>0?j/c:1;if(j===c||Q===0)C=1;Da=(d[0][1]+d[1][1])/2;A()&&function(){var P=ga;ga=C;C=P;P=gb;gb=Da;Da=P;P=Ma;Ma=pa;pa=P}();if(n()[0]*ga>va())ga=va()/n()[0];f=[];for(c=0;c<J();++c)f.push(C);for(c=0;c<J();++c)if(h(c)[3]*f[c]>T(c))f[c]= T(c)/h(c)[3];if(r===-1){if(ga!==1&&(ga<1||n()[0]!==va()))Ba(n(),y([ga,0,0,1,-ga*Ma+gb,0],n()));for(c=0;c<J();++c)if(f[c]!==1&&(f[c]<1||h(c)[3]!==T(c)))Ba(h(c),y([1,0,0,f[c],0,-f[c]*pa+Da],h(c)))}else if(f[r]!==1&&(f[r]<1||h(r)[3]!==T(r)))Ba(h(r),y([1,0,0,f[r],0,-f[r]*pa+Da],h(r)));ea();e=Ra(e,O());z[v]=e[v];z[u]=e[u];s=d;Va();V();Ha()}Xa=null},1))}};this.setXRange=function(a,b,c,e){b=m(0)[0]+m(0)[2]*b;c=m(0)[0]+m(0)[2]*c;if(p(m(0))>q(m(0))){if(b>p(m(0)))b=p(m(0));if(c<q(m(0)))c=q(m(0))}else{if(b< p(m(0)))b=p(m(0));if(c>q(m(0)))c=q(m(0))}var d=Na(a);d=Gb(d,N(a),b,c,A(),g(),m(N(a)),k.maxZoom);b=d.xZoom;c=d.yZoom;d=d.panPoint;var f=ja(z,O());n()[0]=b;if(c&&e)h(N(a))[3]=c;n()[4]=-d[v]*b;if(c&&e)h(N(a))[5]=-d[u]*c;Fa();a=Ra(f,O());z[v]=a[v];z[u]=a[u];ea();Va();V();Ha()};this.getSeries=function(a){return Na(a)};this.rangeChangedCallbacks=[];this.updateConfig=function(a){for(var b in a)if(a.hasOwnProperty(b))k[b]=a[b];Bb();Va();V();Ha()};this.updateConfig({});if(window.TouchEvent&&!window.MSPointerEvent&& !window.PointerEvent){na.touchStart=X.start;na.touchEnd=X.end;na.touchMoved=X.moved}else{G=function(){};na.touchStart=G;na.touchEnd=G;na.touchMoved=G}}");
	}

	private static final int TICK_LENGTH = 5;
	private static final int CURVE_LABEL_PADDING = 10;
	private static final int DEFAULT_CURVE_LABEL_WIDTH = 100;
	private static final int CURVE_SELECTION_DISTANCE_SQUARED = 400;

	static int toZoomLevel(double zoomFactor) {
		return (int) Math.floor(Math.log(zoomFactor) / Math.log(2.0) + 0.5) + 1;
	}
}
