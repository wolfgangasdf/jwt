/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;

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

class StdGridLayoutImpl2 extends StdLayoutImpl {
	private static Logger logger = LoggerFactory
			.getLogger(StdGridLayoutImpl2.class);

	public StdGridLayoutImpl2(WLayout layout, Grid grid) {
		super(layout);
		this.grid_ = grid;
		this.needAdjust_ = false;
		this.needRemeasure_ = false;
		this.needConfigUpdate_ = false;
		this.addedItems_ = new ArrayList<WLayoutItem>();
		this.removedItems_ = new ArrayList<String>();
		String THIS_JS = "js/StdGridLayoutImpl2.js";
		WApplication app = WApplication.getInstance();
		if (!app.isJavaScriptLoaded(THIS_JS)) {
			app.getStyleSheet().addRule("table.Wt-hcenter",
					"margin: 0px auto;position: relative");
			app.loadJavaScript(THIS_JS, wtjs1());
			app.loadJavaScript(THIS_JS, appjs1());
			app.doJavaScript(app.getJavaScriptClass()
					+ ".layouts2.scheduleAdjust();");
			app.doJavaScript("$(window).load(function() { "
					+ app.getJavaScriptClass() + ".layouts2.scheduleAdjust();"
					+ "});");
			WApplication.getInstance().addAutoJavaScript(
					app.getJavaScriptClass() + ".layouts2.adjustNow();");
		}
	}

	public int getMinimumWidth() {
		final int colCount = this.grid_.columns_.size();
		int total = 0;
		for (int i = 0; i < colCount; ++i) {
			total += this.minimumWidthForColumn(i);
		}
		return total + (colCount - 1) * this.grid_.horizontalSpacing_;
	}

	public int getMinimumHeight() {
		final int rowCount = this.grid_.rows_.size();
		int total = 0;
		for (int i = 0; i < rowCount; ++i) {
			total += this.minimumHeightForRow(i);
		}
		return total + (rowCount - 1) * this.grid_.verticalSpacing_;
	}

	public void updateAddItem(WLayoutItem item) {
		super.updateAddItem(item);
		this.addedItems_.add(item);
	}

	public void updateRemoveItem(WLayoutItem item) {
		super.updateRemoveItem(item);
		this.addedItems_.remove(item);
		this.removedItems_.add(getImpl(item).getId());
	}

	public void update(WLayoutItem item) {
		WContainerWidget c = this.getContainer();
		if (c != null) {
			c.layoutChanged(false, false);
		}
		this.needConfigUpdate_ = true;
	}

	public DomElement createDomElement(boolean fitWidth, boolean fitHeight,
			WApplication app) {
		this.needAdjust_ = this.needConfigUpdate_ = this.needRemeasure_ = false;
		this.addedItems_.clear();
		this.removedItems_.clear();
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		int[] margin = { 0, 0, 0, 0 };
		int maxWidth = 0;
		int maxHeight = 0;
		if (this.getLayout().getParentLayout() == null) {
			margin[3] = this.getLayout().getContentsMargin(Side.Left);
			margin[0] = this.getLayout().getContentsMargin(Side.Top);
			margin[1] = this.getLayout().getContentsMargin(Side.Right);
			margin[2] = this.getLayout().getContentsMargin(Side.Bottom);
			maxWidth = pixelSize(this.getContainer().getMaximumWidth());
			maxHeight = pixelSize(this.getContainer().getMaximumHeight());
		}
		StringBuilder js = new StringBuilder();
		js.append(app.getJavaScriptClass()).append(
				".layouts2.add(new Wt3_3_1.StdLayout2(").append(
				app.getJavaScriptClass()).append(",'").append(this.getId())
				.append("',");
		if (this.getLayout().getParentLayout() != null) {
			js.append("'").append(
					getImpl(this.getLayout().getParentLayout()).getId())
					.append("',");
		} else {
			js.append("null,");
		}
		boolean progressive = !app.getEnvironment().hasAjax();
		js.append(fitWidth ? '1' : '0').append(",").append(
				fitHeight ? '1' : '0').append(",").append(
				progressive ? '1' : '0').append(",");
		js.append(maxWidth).append(",").append(maxHeight).append(",[").append(
				this.grid_.horizontalSpacing_).append(",").append(margin[3])
				.append(",").append(margin[1]).append("],[").append(
						this.grid_.verticalSpacing_).append(",").append(
						margin[0]).append(",").append(margin[2]).append("],");
		this.streamConfig(js, app);
		DomElement div = DomElement.createNew(DomElementType.DomElement_DIV);
		div.setId(this.getId());
		div.setProperty(Property.PropertyStylePosition, "relative");
		DomElement table = null;
		DomElement tbody = null;
		DomElement tr = null;
		if (progressive) {
			table = DomElement.createNew(DomElementType.DomElement_TABLE);
			StringBuilder style = new StringBuilder();
			if (maxWidth != 0) {
				style.append("max-width: ").append(maxWidth).append("px;");
			}
			if (maxHeight != 0) {
				style.append("max-height: ").append(maxHeight).append("px;");
			}
			style.append("width: 100%;");
			table.setProperty(Property.PropertyStyle, style.toString());
			int totalColStretch = 0;
			for (int col = 0; col < colCount; ++col) {
				totalColStretch += Math.max(0,
						this.grid_.columns_.get(col).stretch_);
			}
			for (int col = 0; col < colCount; ++col) {
				DomElement c = DomElement
						.createNew(DomElementType.DomElement_COL);
				int stretch = Math
						.max(0, this.grid_.columns_.get(col).stretch_);
				if (stretch != 0 || totalColStretch == 0) {
					char[] buf = new char[30];
					double pct = totalColStretch == 0 ? 100.0 / colCount
							: 100.0 * stretch / totalColStretch;
					StringBuilder ss = new StringBuilder();
					ss.append("width:").append(MathUtils.roundCss(pct, 2))
							.append("%;");
					c.setProperty(Property.PropertyStyle, ss.toString());
				}
				table.addChild(c);
			}
			tbody = DomElement.createNew(DomElementType.DomElement_TBODY);
		}
		List<Boolean> overSpanned = new ArrayList<Boolean>();
		{
			int insertPos = 0;
			for (int ii = 0; ii < colCount * rowCount; ++ii)
				overSpanned.add(insertPos + ii, false);
		}
		;
		int prevRowWithItem = -1;
		for (int row = 0; row < rowCount; ++row) {
			if (table != null) {
				tr = DomElement.createNew(DomElementType.DomElement_TR);
			}
			boolean rowVisible = false;
			int prevColumnWithItem = -1;
			for (int col = 0; col < colCount; ++col) {
				Grid.Item item = this.grid_.items_.get(row).get(col);
				if (!overSpanned.get(row * colCount + col)) {
					for (int i = 0; i < item.rowSpan_; ++i) {
						for (int j = 0; j < item.colSpan_; ++j) {
							if (i + j > 0) {
								overSpanned.set((row + i) * colCount + col + j,
										true);
							}
						}
					}
					AlignmentFlag hAlign = EnumUtils.enumFromSet(EnumUtils
							.mask(item.alignment_,
									AlignmentFlag.AlignHorizontalMask));
					AlignmentFlag vAlign = EnumUtils.enumFromSet(EnumUtils
							.mask(item.alignment_,
									AlignmentFlag.AlignVerticalMask));
					DomElement td = null;
					if (table != null) {
						boolean itemVisible = this.hasItem(row, col);
						rowVisible = rowVisible || itemVisible;
						td = DomElement.createNew(DomElementType.DomElement_TD);
						if (itemVisible) {
							int[] padding = { 0, 0, 0, 0 };
							int nextRow = this.nextRowWithItem(row, col);
							int prevRow = prevRowWithItem;
							int nextCol = this.nextColumnWithItem(row, col);
							int prevCol = prevColumnWithItem;
							if (prevRow == -1) {
								padding[0] = margin[0];
							} else {
								padding[0] = (this.grid_.verticalSpacing_ + 1) / 2;
							}
							if (nextRow == (int) rowCount) {
								padding[2] = margin[2];
							} else {
								padding[2] = this.grid_.verticalSpacing_ / 2;
							}
							if (prevCol == -1) {
								padding[3] = margin[3];
							} else {
								padding[3] = (this.grid_.horizontalSpacing_ + 1) / 2;
							}
							if (nextCol == (int) colCount) {
								padding[1] = margin[1];
							} else {
								padding[1] = this.grid_.horizontalSpacing_ / 2;
							}
							StringBuilder style = new StringBuilder();
							if (app.getLayoutDirection() == LayoutDirection.RightToLeft) {
								int tmp = padding[1];
								padding[1] = padding[3];
								padding[3] = tmp;
								;
							}
							if (padding[0] == padding[1]
									&& padding[0] == padding[2]
									&& padding[0] == padding[3]) {
								if (padding[0] != 0) {
									style.append("padding:").append(padding[0])
											.append("px;");
								}
							} else {
								style.append("padding:").append(padding[0])
										.append("px ").append(padding[1])
										.append("px ").append(padding[2])
										.append("px ").append(padding[3])
										.append("px;");
							}
							if (vAlign != null) {
								switch (vAlign) {
								case AlignTop:
									style.append("vertical-align:top;");
									break;
								case AlignMiddle:
									style.append("vertical-align:middle;");
									break;
								case AlignBottom:
									style.append("vertical-align:bottom;");
								default:
									break;
								}
							}
							td.setProperty(Property.PropertyStyle, style
									.toString());
							if (item.rowSpan_ != 1) {
								td.setProperty(Property.PropertyRowSpan, String
										.valueOf(item.rowSpan_));
							}
							if (item.colSpan_ != 1) {
								td.setProperty(Property.PropertyColSpan, String
										.valueOf(item.colSpan_));
							}
							prevColumnWithItem = col;
						}
					}
					DomElement c = null;
					if (!(table != null)) {
						if (item.item_ != null) {
							c = this.createElement(item.item_, app);
							div.addChild(c);
						}
					} else {
						if (item.item_ != null) {
							c = getImpl(item.item_).createDomElement(true,
									true, app);
						}
					}
					if (table != null) {
						if (c != null) {
							if (!app.getEnvironment().agentIsIE()) {
								c.setProperty(Property.PropertyStyleBoxSizing,
										"border-box");
							}
							if (hAlign == null) {
								hAlign = AlignmentFlag.AlignJustify;
							}
							switch (hAlign) {
							case AlignCenter: {
								DomElement itable = DomElement
										.createNew(DomElementType.DomElement_TABLE);
								itable.setProperty(Property.PropertyClass,
										"Wt-hcenter");
								if (vAlign == null) {
									itable.setProperty(Property.PropertyStyle,
											"height:100%;");
								}
								DomElement irow = DomElement
										.createNew(DomElementType.DomElement_TR);
								DomElement itd = DomElement
										.createNew(DomElementType.DomElement_TD);
								if (vAlign == null) {
									itd.setProperty(Property.PropertyStyle,
											"height:100%;");
								}
								itd.addChild(c);
								if (app.getEnvironment().agentIsIElt(9)) {
									if (c.getProperty(
											Property.PropertyStyleMinWidth)
											.length() != 0) {
										DomElement spacer = DomElement
												.createNew(DomElementType.DomElement_DIV);
										spacer
												.setProperty(
														Property.PropertyStyleWidth,
														c
																.getProperty(Property.PropertyStyleMinWidth));
										spacer.setProperty(
												Property.PropertyStyleHeight,
												"1px");
										itd.addChild(spacer);
									}
								}
								irow.addChild(itd);
								itable.addChild(irow);
								c = itable;
								break;
							}
							case AlignRight:
								if (!c.isDefaultInline()) {
									c.setProperty(Property.PropertyStyleFloat,
											"right");
								} else {
									td.setProperty(
											Property.PropertyStyleTextAlign,
											"right");
								}
								break;
							case AlignLeft:
								if (!c.isDefaultInline()) {
									c.setProperty(Property.PropertyStyleFloat,
											"left");
								} else {
									td.setProperty(
											Property.PropertyStyleTextAlign,
											"left");
								}
								break;
							default:
								break;
							}
							td.addChild(c);
							if (app.getEnvironment().agentIsIElt(9)) {
								if (c.getProperty(
										Property.PropertyStyleMinWidth)
										.length() != 0) {
									DomElement spacer = DomElement
											.createNew(DomElementType.DomElement_DIV);
									spacer
											.setProperty(
													Property.PropertyStyleWidth,
													c
															.getProperty(Property.PropertyStyleMinWidth));
									spacer
											.setProperty(
													Property.PropertyStyleHeight,
													"1px");
									td.addChild(spacer);
								}
							}
						}
						tr.addChild(td);
					}
				}
			}
			if (tr != null) {
				if (!rowVisible) {
					tr.setProperty(Property.PropertyStyleDisplay, "hidden");
				} else {
					prevRowWithItem = row;
				}
				tbody.addChild(tr);
			}
		}
		js.append("));");
		if (table != null) {
			table.addChild(tbody);
			div.addChild(table);
		}
		div.callJavaScript(js.toString());
		return div;
	}

	public void updateDom(DomElement parent) {
		WApplication app = WApplication.getInstance();
		if (this.needConfigUpdate_) {
			this.needConfigUpdate_ = false;
			DomElement div = DomElement.getForUpdate(this,
					DomElementType.DomElement_DIV);
			for (int i = 0; i < this.addedItems_.size(); ++i) {
				WLayoutItem item = this.addedItems_.get(i);
				DomElement c = this.createElement(item, app);
				div.addChild(c);
			}
			this.addedItems_.clear();
			for (int i = 0; i < this.removedItems_.size(); ++i) {
				parent.callJavaScript("Wt3_3_1.remove('"
						+ this.removedItems_.get(i) + "');", true);
			}
			this.removedItems_.clear();
			parent.addChild(div);
			StringBuilder js = new StringBuilder();
			js.append(app.getJavaScriptClass()).append(
					".layouts2.updateConfig('").append(this.getId()).append(
					"',");
			this.streamConfig(js, app);
			js.append(");");
			app.doJavaScript(js.toString());
		}
		if (this.needRemeasure_) {
			this.needRemeasure_ = false;
			StringBuilder js = new StringBuilder();
			js.append(app.getJavaScriptClass()).append(".layouts2.setDirty('")
					.append(this.getId()).append("');");
			app.doJavaScript(js.toString());
		}
		if (this.needAdjust_) {
			this.needAdjust_ = false;
			StringBuilder js = new StringBuilder();
			js.append(app.getJavaScriptClass()).append(".layouts2.adjust('")
					.append(this.getId()).append("', [");
			boolean first = true;
			final int colCount = this.grid_.columns_.size();
			final int rowCount = this.grid_.rows_.size();
			for (int row = 0; row < rowCount; ++row) {
				for (int col = 0; col < colCount; ++col) {
					if (this.grid_.items_.get(row).get(col).update_) {
						this.grid_.items_.get(row).get(col).update_ = false;
						if (!first) {
							js.append(",");
						}
						first = false;
						js.append("[").append((int) row).append(",").append(
								(int) col).append("]");
					}
				}
			}
			js.append("]);");
			app.doJavaScript(js.toString());
		}
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		for (int i = 0; i < rowCount; ++i) {
			for (int j = 0; j < colCount; ++j) {
				WLayoutItem item = this.grid_.items_.get(i).get(j).item_;
				if (item != null) {
					WLayout nested = item.getLayout();
					if (nested != null) {
						(((nested.getImpl()) instanceof StdLayoutImpl ? (StdLayoutImpl) (nested
								.getImpl())
								: null)).updateDom(parent);
					}
				}
			}
		}
	}

	public void setHint(String name, String value) {
		logger.error(new StringWriter().append("unrecognized hint '").append(
				name).append("'").toString());
	}

	public boolean itemResized(WLayoutItem item) {
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		for (int row = 0; row < rowCount; ++row) {
			for (int col = 0; col < colCount; ++col) {
				if (this.grid_.items_.get(row).get(col).item_ == item
						&& !this.grid_.items_.get(row).get(col).update_) {
					this.grid_.items_.get(row).get(col).update_ = true;
					this.needAdjust_ = true;
					return true;
				}
			}
		}
		return false;
	}

	public boolean isParentResized() {
		if (!this.needRemeasure_) {
			this.needRemeasure_ = true;
			return true;
		} else {
			return false;
		}
	}

	void containerAddWidgets(WContainerWidget container) {
		super.containerAddWidgets(container);
		if (!(container != null)) {
			return;
		}
		WApplication app = WApplication.getInstance();
		if (this.getParentLayoutImpl() == null) {
			if (container == app.getRoot()) {
				app.setBodyClass(app.getBodyClass() + " Wt-layout");
				app.setHtmlClass(app.getHtmlClass() + " Wt-layout");
			}
		}
	}

	private Grid grid_;
	private boolean needAdjust_;
	private boolean needRemeasure_;
	private boolean needConfigUpdate_;
	private List<WLayoutItem> addedItems_;
	private List<String> removedItems_;

	private int nextRowWithItem(int row, int c) {
		for (row += this.grid_.items_.get(row).get(c).rowSpan_; row < (int) this.grid_.rows_
				.size(); ++row) {
			for (int col = 0; col < this.grid_.columns_.size(); col += this.grid_.items_
					.get(row).get(col).colSpan_) {
				if (this.hasItem(row, col)) {
					return row;
				}
			}
		}
		return this.grid_.rows_.size();
	}

	private int nextColumnWithItem(int row, int col) {
		for (;;) {
			col = col + this.grid_.items_.get(row).get(col).colSpan_;
			if (col < (int) this.grid_.columns_.size()) {
				for (int i = 0; i < this.grid_.rows_.size(); ++i) {
					if (this.hasItem(i, col)) {
						return col;
					}
				}
			} else {
				return this.grid_.columns_.size();
			}
		}
	}

	private boolean hasItem(int row, int col) {
		WLayoutItem item = this.grid_.items_.get(row).get(col).item_;
		if (item != null) {
			WWidget w = item.getWidget();
			return !(w != null) || !w.isHidden();
		} else {
			return false;
		}
	}

	private int minimumHeightForRow(int row) {
		int minHeight = 0;
		final int colCount = this.grid_.columns_.size();
		for (int j = 0; j < colCount; ++j) {
			WLayoutItem item = this.grid_.items_.get(row).get(j).item_;
			if (item != null) {
				minHeight = Math.max(minHeight, getImpl(item)
						.getMinimumHeight());
			}
		}
		return minHeight;
	}

	private int minimumWidthForColumn(int col) {
		int minWidth = 0;
		final int rowCount = this.grid_.rows_.size();
		for (int i = 0; i < rowCount; ++i) {
			WLayoutItem item = this.grid_.items_.get(i).get(col).item_;
			if (item != null) {
				minWidth = Math.max(minWidth, getImpl(item).getMinimumWidth());
			}
		}
		return minWidth;
	}

	private static int pixelSize(WLength size) {
		if (size.getUnit() == WLength.Unit.Percentage) {
			return 0;
		} else {
			return (int) size.toPixels();
		}
	}

	private void streamConfig(StringBuilder js, List<Grid.Section> sections,
			boolean rows, WApplication app) {
		js.append("[");
		for (int i = 0; i < sections.size(); ++i) {
			if (i != 0) {
				js.append(",");
			}
			js.append("[").append(sections.get(i).stretch_).append(",");
			if (sections.get(i).resizable_) {
				SizeHandle.loadJavaScript(app);
				js.append("[");
				WLength size = sections.get(i).initialSize_;
				if (size.isAuto()) {
					js.append("-1");
				} else {
					if (size.getUnit() == WLength.Unit.Percentage) {
						js.append(size.getValue()).append(",1");
					} else {
						js.append(size.toPixels());
					}
				}
				js.append("],");
			} else {
				js.append("0,");
			}
			if (rows) {
				js.append(this.minimumHeightForRow(i));
			} else {
				js.append(this.minimumWidthForColumn(i));
			}
			js.append("]");
		}
		js.append("]");
	}

	private void streamConfig(StringBuilder js, WApplication app) {
		js.append("{ rows:");
		this.streamConfig(js, this.grid_.rows_, true, app);
		js.append(", cols:");
		this.streamConfig(js, this.grid_.columns_, false, app);
		js.append(", items: [");
		final int colCount = this.grid_.columns_.size();
		final int rowCount = this.grid_.rows_.size();
		for (int row = 0; row < rowCount; ++row) {
			for (int col = 0; col < colCount; ++col) {
				Grid.Item item = this.grid_.items_.get(row).get(col);
				AlignmentFlag hAlign = EnumUtils.enumFromSet(EnumUtils.mask(
						item.alignment_, AlignmentFlag.AlignHorizontalMask));
				AlignmentFlag vAlign = EnumUtils.enumFromSet(EnumUtils.mask(
						item.alignment_, AlignmentFlag.AlignVerticalMask));
				if (row + col != 0) {
					js.append(",");
				}
				if (item.item_ != null) {
					String id = getImpl(item.item_).getId();
					js.append("{");
					if (item.colSpan_ != 1 || item.rowSpan_ != 1) {
						js.append("span: [").append(item.colSpan_).append(",")
								.append(item.rowSpan_).append("],");
					}
					if (!item.alignment_.isEmpty()) {
						int align = 0;
						if (hAlign != null) {
							switch (hAlign) {
							case AlignLeft:
								align |= 0x1;
								break;
							case AlignRight:
								align |= 0x2;
								break;
							case AlignCenter:
								align |= 0x4;
								break;
							default:
								break;
							}
						}
						if (vAlign != null) {
							switch (vAlign) {
							case AlignTop:
								align |= 0x10;
								break;
							case AlignBottom:
								align |= 0x20;
								break;
							case AlignMiddle:
								align |= 0x40;
								break;
							default:
								break;
							}
						}
						js.append("align:").append((int) align).append(",");
					}
					js.append("dirty:2,id:'").append(id).append("'")
							.append("}");
				} else {
					js.append("null");
				}
			}
		}
		js.append("]}");
	}

	private DomElement createElement(WLayoutItem item, WApplication app) {
		DomElement c = getImpl(item).createDomElement(true, true, app);
		c.setProperty(Property.PropertyStyleVisibility, "hidden");
		return c;
	}

	static WJavaScriptPreamble wtjs1() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptConstructor,
				"StdLayout2",
				"function(D,I,K,P,Q,M,V,t,H,w,z){function A(a,b,c,e){function h(n){return n==\"visible\"||n==\"none\"}var j=v[b],l=b?a.scrollHeight:a.scrollWidth,u,d;if(b==0&&l+g.pxself(a,j.left)>=e.clientWidth){u=a.style[j.left];x(a,j.left,\"-1000000px\");l=b?a.scrollHeight:a.scrollWidth}e=b?a.clientHeight:a.clientWidth;if(g.isGecko&&b==0&&h(g.css(a,\"overflow\"))){d=a.style[j.size];x(a,j.size,\"\")}b=b?a.offsetHeight:a.offsetWidth;u&&x(a,j.left,u);d&&x(a,j.size,d); if(e>=1E6)e-=1E6;if(l>=1E6)l-=1E6;if(b>=1E6)b-=1E6;if(l===0){l=g.pxself(a,j.size);if(l!==0&&!g.isOpera&&!g.isGecko)l-=g.px(a,\"border\"+j.Left+\"Width\")+g.px(a,\"border\"+j.Right+\"Width\")}if(g.isIE&&(g.hasTag(a,\"BUTTON\")||g.hasTag(a,\"TEXTAREA\")||g.hasTag(a,\"INPUT\")||g.hasTag(a,\"SELECT\")))l=e;if(l>b)if(g.pxself(a,j.size)==0)l=e;else{var p=false;$(a).find(\".Wt-popup\").each(function(){if(this.style.display!==\"none\")p=true});if(p)l=e}d=g.px(a,\"border\"+j.Left+\"Width\")+g.px(a,\"border\"+j.Right+\"Width\");u=b-(e+ d)!=0;if(c)return[l,scrollBar];g.isOpera||(l+=d);l+=g.px(a,\"margin\"+j.Left)+g.px(a,\"margin\"+j.Right);if(!g.boxSizing(a)&&!g.isIE)l+=g.px(a,\"padding\"+j.Left)+g.px(a,\"padding\"+j.Right);l+=b-(e+d);if(l<b)l=b;a=g.px(a,\"max\"+j.Size);if(a>0)l=Math.min(a,l);return[Math.round(l),u]}function E(a,b){b=v[b];if(a.style.display===\"none\")return 0;else if(a[\"layoutMin\"+b.Size])return a[\"layoutMin\"+b.Size];else{var c=g.px(a,\"min\"+b.Size);g.boxSizing(a)||(c+=g.px(a,\"padding\"+b.Left)+g.px(a,\"padding\"+b.Right));return c}} function y(a,b){b=v[b];var c=g.px(a,\"margin\"+b.Left)+g.px(a,\"margin\"+b.Right);if(!g.boxSizing(a)&&!(g.isIE&&!g.isIElt9&&g.hasTag(a,\"BUTTON\")))c+=g.px(a,\"border\"+b.Left+\"Width\")+g.px(a,\"border\"+b.Right+\"Width\")+g.px(a,\"padding\"+b.Left)+g.px(a,\"padding\"+b.Right);return c}function L(a,b){b=v[b];return g.px(a,\"padding\"+b.Left)+g.px(a,\"padding\"+b.Right)}function J(a,b){if(g.boxSizing(a)){b=v[b];return g.px(a,\"border\"+b.Left+\"Width\")+g.px(a,\"border\"+b.Right+\"Width\")+g.px(a,\"padding\"+b.Left)+g.px(a,\"padding\"+ b.Right)}else return 0}function W(a,b){b=v[b];return Math.round(g.px(a,\"border\"+b.Left+\"Width\")+g.px(a,\"border\"+b.Right+\"Width\")+g.px(a,\"margin\"+b.Left)+g.px(a,\"margin\"+b.Right)+g.px(a,\"padding\"+b.Left)+g.px(a,\"padding\"+b.Right))}function R(a,b,c){a.dirty=Math.max(a.dirty,b);N=true;c&&D.layouts2.scheduleAdjust()}function x(a,b,c){if(a.style[b]!==c){a.style[b]=c;return true}else return false}function ea(a,b,c){var e=v[a],h=e.config.length,j=v[a^1].config.length,l=e.measures.slice();if(l.length==5){l[0]= l[0].slice();l[1]=l[1].slice()}if(N){if(c&&typeof e.minSize==\"undefined\"){e.minSize=g.px(c,\"min\"+e.Size);if(e.minSize>0)e.minSize-=J(c,a)}var u=[],d=[],p=0,n=0,k,G,s=false;for(k=0;k<h;++k){var m=0,r=e.config[k][2],o=true;for(G=0;G<j;++G){var f=e.getItem(k,G);if(f){if(!f.w||a==0&&f.dirty>1){var q=$(\"#\"+f.id),B=q.get(0);if(!B){e.setItem(k,G,null);continue}if(B!=f.w){f.w=B;q.find(\"img\").add(q.filter(\"img\")).bind(\"load\",{item:f},function(fa){R(fa.data.item,1,true)})}}if(!M&&f.w.style.position!=\"absolute\"){f.w.style.position= \"absolute\";f.w.style.visibility=\"hidden\"}if(!f.ps)f.ps=[];if(!f.sc)f.sc=[];if(!f.ms)f.ms=[];if(!f.size)f.size=[];if(!f.psize)f.psize=[];if(!f.fs)f.fs=[];if(!f.margin)f.margin=[];if($(f.w).hasClass(\"Wt-hidden\"))f.ps[a]=f.ms[a]=0;else{q=!f.set;if(!f.set)f.set=[false,false];if(f.w){if(g.isIE)f.w.style.visibility=\"\";if(f.dirty){var F;if(f.dirty>1){E(f.w,a);f.ms[a]=F}else F=f.ms[a];if(F>r)r=F;if(f.dirty>1)f.margin[a]=y(f.w,a);if(!f.set[a])if(a==0||!q){q=g.pxself(f.w,e.size);f.fs[a]=q?q+f.margin[a]:0}else{q= Math.round(g.px(f.w,e.size));f.fs[a]=q>Math.max(J(f.w,a),F)?q+f.margin[a]:0}q=f.fs[a];if(f.layout){if(q==0)q=f.ps[a];f.ps[a]=q}else{if(f.wasLayout){f.wasLayout=false;f.set=[false,false];f.ps=[];f.w.wtResize&&f.w.wtResize(f.w,-1,-1,true);x(f.w,v[1].size,\"\")}B=A(f.w,a,false,b);var O=B[0],X=f.set[a];if(X)if(f.psize[a]>8)X=O>=f.psize[a]-4&&O<=f.psize[a]+4;var ga=typeof f.ps[a]!==\"undefined\"&&e.config[k][0]>0&&f.set[a];q=X||ga?Math.max(q,f.ps[a]):Math.max(q,O);f.ps[a]=q;f.sc[a]=B[1]}if(!f.span||f.span[a]== 1){if(q>m)m=q}else s=true}else if(!f.span||f.span[a]==1){if(f.ps[a]>m)m=f.ps[a];if(f.ms[a]>r)r=f.ms[a]}else s=true;if(!(f.w.style.display===\"none\"&&!f.w.ed)&&(!f.span||f.span[a]==1))o=false}}}}if(o)r=m=-1;else if(r>m)m=r;u[k]=m;d[k]=r;if(r>-1){p+=m;n+=r}}if(s)for(k=0;k<h;++k)for(G=0;G<j;++G)if((f=e.getItem(k,G))&&f.span&&f.span[a]>1){b=f.ps[a];for(r=m=s=0;r<f.span[a];++r){o=u[k+r];if(o!=-1){b-=o;++s;if(e.config[k+r][0]>0)m+=e.config[k+r][0]}}if(b>0)if(s>0){if(m>0)s=m;for(r=0;r<f.span[a];++r){o=u[k+ r];if(o!=-1){o=m>0?e.config[k+r][0]:1;if(o>0){F=Math.round(b/o);b-=F;s-=o;u[k+r]+=F}}}}else u[k]=b}j=0;q=true;G=false;for(k=0;k<h;++k)if(d[k]>-1){if(q){j+=e.margins[1];q=false}else{j+=e.margins[0];if(G)j+=4}G=e.config[k][1]!==0}q||(j+=e.margins[2]);p+=j;n+=j;e.measures=[u,d,p,n,j]}if(Y||l[2]!=e.measures[2])S.updateSizeInParent(a);c&&e.minSize==0&&l[3]!=e.measures[3]&&c.parentNode.className!=\"Wt-domRoot\"&&x(c,\"min\"+e.Size,e.measures[3]+\"px\")&&S.ancestor&&S.ancestor.setContentsDirty(c);c&&a==0&&c&& g.hasTag(c,\"TD\")&&x(c,e.size,e.measures[2]+\"px\")}function ha(a,b,c){a=v[a];if(Z)c=-c;if(a.config[b][0]>0&&a.config[b+1][0]==0){++b;c=-c}a.fixedSize[b]=a.sizes[b]+c;D.layouts2.scheduleAdjust()}function ia(a,b,c){var e=b.di,h=v[a],j=v[a^1],l,u=g.getElement(I),d;for(d=e-1;d>=0;--d)if(h.sizes[d]>=0){l=-(h.sizes[d]-h.measures[1][d]);break}e=h.sizes[e]-h.measures[1][e];if(Z){var p=l;l=-e;e=-p}new g.SizeHandle(g,h.resizeDir,g.pxself(b,h.size),g.pxself(b,j.size),l,e,h.resizerClass,function(n){ha(a,d,n)}, b,u,c,0,0)}function ja(a,b){var c=v[a],e=v[a^1],h=c.measures,j=0,l=false,u=false,d=false,p=ba?b.parentNode:null;if(c.maxSize===0)if(p){var n=g.css(p,\"position\");if(n===\"absolute\")j=g.pxself(p,c.size);if(j===0){if(!c.initialized){if(a===0&&(n===\"absolute\"||n===\"fixed\")){p.style.display=\"none\";j=p.clientWidth;p.style.display=\"\"}j=a?p.clientHeight:p.clientWidth;l=true;if(a==0&&j==0&&g.isIElt9){j=p.offsetWidth;l=false}var k;if((g.hasTag(p,\"TD\")||g.hasTag(p,\"TH\"))&&!(g.isIE&&!g.isIElt9)){d=0;k=1}else{d= c.minSize?c.minSize:h[3];k=0}function G(O,X){return Math.abs(O-X)<1}if(g.isIElt9&&G(j,k)||G(j,d+L(p,a)))c.maxSize=999999}if(j===0&&c.maxSize===0){j=a?p.clientHeight:p.clientWidth;l=true}}}else{j=g.pxself(b,c.size);u=true}else if(c.sizeSet){j=g.pxself(p,c.size);u=true}var s=0;if(p&&p.wtGetPS&&a==1)s=p.wtGetPS(p,b,a,0);d=h[2];if(d<c.minSize)d=c.minSize;if(c.maxSize&&!c.sizeSet){j=Math.min(d,c.maxSize)+s;x(p,c.size,j+J(p,a)+\"px\")&&D.layouts2.remeasure();j=j;d=u=true}c.cSize=j;if(a==1&&p&&p.wtResize){k= e.cSize;d=c.cSize;p.wtResize(p,Math.round(k),Math.round(d),true)}j-=s;if(!u){u=0;if(typeof c.cPadding===\"undefined\"){u=l?L(p,a):J(p,a);c.cPadding=u}else u=c.cPadding;j-=u}c.initialized=true;if(!(p&&j<=0)){if(j<h[3]-s)j=h[3]-s;l=[];p=c.config.length;u=e.config.length;for(d=0;d<p;++d)c.stretched[d]=false;if(j>=h[3]-s){s=j-h[4];k=[];var m=[0,0],r=[0,0],o=0;for(d=0;d<p;++d)if(h[1][d]>-1){n=-1;if(typeof c.fixedSize[d]!==\"undefined\"&&(d+1==p||h[1][d+1]>-1))n=c.fixedSize[d];else if(c.config[d][1]!==0&&c.config[d][1][0]>= 0){n=c.config[d][1][0];if(c.config[d][1][1])n=(j-h[4])*n/100}if(n>=0){k[d]=-1;l[d]=n;s-=l[d]}else{if(c.config[d][0]>0){n=1;k[d]=c.config[d][0];o+=k[d]}else{n=0;k[d]=0}m[n]+=h[1][d];r[n]+=h[0][d];l[d]=h[0][d]}}else{k[d]=-2;l[d]=-1}if(o==0){for(d=0;d<p;++d)if(k[d]==0){k[d]=1;++o}r[1]=r[0];m[1]=m[0];r[0]=0;m[0]=0}if(s>r[0]+m[1]){s-=r[0];if(s>r[1]){if(c.fitSize){s-=r[1];s=s/o;for(d=0;d<p;++d)if(k[d]>0){l[d]+=Math.round(k[d]*s);c.stretched[d]=true}}}else{n=1;if(s<m[n])s=m[n];s=r[n]-m[n]>0?(s-m[n])/(r[n]- m[n]):0;for(d=0;d<p;++d)if(k[d]>0){m=h[0][d]-h[1][d];l[d]=h[1][d]+Math.round(m*s)}}}else{for(d=0;d<p;++d)if(k[d]>0)l[d]=h[1][d];s-=m[1];n=0;if(s<m[n])s=m[n];s=r[n]-m[n]>0?(s-m[n])/(r[n]-m[n]):0;for(d=0;d<p;++d)if(k[d]==0){m=h[0][d]-h[1][d];l[d]=h[1][d]+Math.round(m*s)}}}else l=h[1];c.sizes=l;h=c.margins[1];s=true;m=false;for(d=0;d<p;++d){if(l[d]>-1){var f=m;if(m){k=I+\"-rs\"+a+\"-\"+d;m=g.getElement(k);if(!m){m=document.createElement(\"div\");m.setAttribute(\"id\",k);m.di=d;m.style.position=\"absolute\";m.style[e.left]= e.margins[1]+\"px\";m.style[c.size]=c.margins[0]+\"px\";if(e.cSize)m.style[e.size]=e.cSize-e.margins[2]-e.margins[1]+\"px\";m.className=c.handleClass;b.insertBefore(m,b.firstChild);m.onmousedown=m.ontouchstart=function(O){ia(a,this,O||window.event)}}h+=2;x(m,c.left,h+\"px\");h+=2}m=c.config[d][1]!==0;if(s)s=false;else h+=c.margins[0]}for(r=0;r<u;++r)if((o=c.getItem(d,r))&&o.w){k=o.w;n=Math.max(l[d],0);if(o.span){var q,B=m;for(q=1;q<o.span[a];++q){if(d+q>=l.length)break;if(B)n+=4;B=c.config[d+q][1]!==0;if(l[d+ q-1]>-1&&l[d+q]>-1)n+=c.margins[0];n+=l[d+q]}}var F;x(k,\"visibility\",\"\");B=o.align>>c.alignBits&15;q=o.ps[a];if(n<q)B=0;if(B){switch(B){case 1:F=h;break;case 4:F=h+(n-q)/2;break;case 2:F=h+(n-q);break}q-=o.margin[a];if(o.layout){x(k,c.size,q+\"px\")&&R(o,1);o.set[a]=true}else if(n>=q&&o.set[a]){x(k,c.size,q+\"px\")&&R(o,1);o.set[a]=false}o.size[a]=q;o.psize[a]=q}else{B=Math.max(0,n-o.margin[a]);if(!g.isIE&&g.hasTag(k,\"TEXTAREA\"))B=n;F=o.sc[a];if(!(k.style.display===\"none\"&&!k.ed)&&(F||n!=q||o.layout)){if(x(k, c.size,B+\"px\")){R(o,1);o.set[a]=true}}else if(o.fs[a])a==0&&x(k,c.size,o.fs[a]+\"px\");else{x(k,c.size,\"\")&&R(o,1);o.set[a]=false}F=h;o.size[a]=B;o.psize[a]=n}if(M)if(f){x(k,c.left,\"4px\");n=g.css(k,\"position\");if(n!==\"absolute\")k.style.position=\"relative\"}else x(k,c.left,\"0px\");else x(k,c.left,F+\"px\");if(a==1){if(k.wtResize)k.wtResize(k,o.set[0]?Math.round(o.size[0]):-1,o.set[1]?Math.round(o.size[1]):-1,true);o.dirty=0}}if(l[d]>-1)h+=l[d]}$(b).children(\".\"+e.handleClass).css(c.size,j-c.margins[2]-c.margins[1]+ \"px\")}}var g=D.WT;this.ancestor=null;this.descendants=[];var S=this,C=z,N=true,Y=true,ba=false,aa=null,T=null,ca=false,U=[],da=false,Z=$(document.body).hasClass(\"Wt-rtl\"),v=[{initialized:false,config:C.cols,margins:H,maxSize:V,measures:[],sizes:[],stretched:[],fixedSize:[],Left:Z?\"Right\":\"Left\",left:Z?\"right\":\"left\",Right:Z?\"Left\":\"Right\",Size:\"Width\",size:\"width\",alignBits:0,getItem:function(a,b){return C.items[b*v[0].config.length+a]},setItem:function(a,b,c){C.items[b*v[0].config.length+a]=c},handleClass:\"Wt-vrh2\", resizeDir:\"h\",resizerClass:\"Wt-hsh2\",fitSize:P},{initialized:false,config:C.rows,margins:w,maxSize:t,measures:[],sizes:[],stretched:[],fixedSize:[],Left:\"Top\",left:\"top\",Right:\"Bottom\",Size:\"Height\",size:\"height\",alignBits:4,getItem:function(a,b){return C.items[a*v[0].config.length+b]},setItem:function(a,b,c){C.items[a*v[0].config.length+b]=c},handleClass:\"Wt-hrh2\",resizeDir:\"v\",resizerClass:\"Wt-vsh2\",fitSize:Q}];jQuery.data(document.getElementById(I),\"layout\",this);this.updateSizeInParent=function(a){if(aa){var b= v[a],c=b.measures[2];if(b.maxSize>0)c=Math.min(b.maxSize,c);if(da){b=g.getElement(I);if(!b)return;for(var e=b,h=e.parentNode;;){if(h.wtGetPS)c=h.wtGetPS(h,e,a,c);c+=W(h,a);if(h==T)break;if(a==1&&h==b.parentNode&&!h.lh&&h.offsetHeight>c)c=h.offsetHeight;e=h;h=e.parentNode}}else c+=U[a];aa.setChildSize(T,a,c)}};this.setConfig=function(a){var b=C;C=a;v[0].config=C.cols;v[1].config=C.rows;v[0].stretched=[];v[1].stretched=[];var c;a=0;for(c=b.items.length;a<c;++a){var e=b.items[a];if(e){if(e.set){e.set[0]&& x(e.w,v[0].size,\"\");e.set[1]&&x(e.w,v[1].size,\"\")}if(e.layout){S.setChildSize(e.w,0,e.ps[0]);S.setChildSize(e.w,1,e.ps[1])}}}Y=true;D.layouts2.scheduleAdjust()};this.getId=function(){return I};this.setElDirty=function(a){var b,c;b=0;for(c=C.items.length;b<c;++b){var e=C.items[b];if(e&&e.id==a.id){e.dirty=2;N=true;return}}};this.setItemsDirty=function(a){var b,c,e=v[0].config.length;b=0;for(c=a.length;b<c;++b){var h=C.items[a[b][0]*e+a[b][1]];h.dirty=2;if(h.layout){h.layout=false;h.wasLayout=true; D.layouts2.setChildLayoutsDirty(S,h.w)}}N=true};this.setDirty=function(){Y=true};this.setAllDirty=function(){i=0;for(il=C.items.length;i<il;++i){var a=C.items[i];if(a)a.dirty=2}N=true};this.setChildSize=function(a,b,c){var e=v[0].config.length,h=v[b],j,l;j=0;for(l=C.items.length;j<l;++j){var u=C.items[j];if(u&&u.id==a.id){a=b===0?j%e:j/e;if(u.align>>h.alignBits&15||!h.stretched[a]){if(!u.ps)u.ps=[];u.ps[b]=c}u.layout=true;R(u,1);break}}};this.measure=function(a){var b=g.getElement(I);if(b)if(!g.isHidden(b)){if(!ca){ca= true;if(ba=K==null){var c=b;c=c.parentNode;for(U=[0,0];;){U[0]+=W(c,0);U[1]+=W(c,1);if(c.wtGetPS)da=true;var e=jQuery.data(c.parentNode,\"layout\");if(e){aa=e;T=c;break}c=c;c=c.parentNode;if(c.childNodes.length!=1&&!c.wtGetPS)break}c=b.parentNode;for(e=0;e<2;++e)v[e].sizeSet=g.pxself(c,v[e].size)!=0}else{aa=jQuery.data(document.getElementById(K),\"layout\");T=b;U[0]=W(T,0);U[1]=W(T,1)}}if(N||Y){c=ba?b.parentNode:null;ea(a,b,c)}if(a==1)N=Y=false}};this.setMaxSize=function(a,b){v[0].maxSize=a;v[1].maxSize= b};this.apply=function(a){var b=g.getElement(I);if(!b)return false;if(g.isHidden(b))return true;ja(a,b);return true};this.contains=function(a){var b=g.getElement(I);a=g.getElement(a.getId());return b&&a?g.contains(b,a):false};this.WT=g}");
	}

	static WJavaScriptPreamble wtjs2() {
		return new WJavaScriptPreamble(JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptPrototype,
				"StdLayout2.prototype.initResize",
				"function(){this.resizeInitialized=true}");
	}

	static WJavaScriptPreamble appjs1() {
		return new WJavaScriptPreamble(
				JavaScriptScope.ApplicationScope,
				JavaScriptObjectType.JavaScriptObject,
				"layouts2",
				"new (function(){var D=[],I=false,K=this,P=false;this.find=function(t){return jQuery.data(document.getElementById(t),\"layout\")};this.setDirty=function(t){if(t=this.find(t)){t.setDirty();K.scheduleAdjust()}};this.setElementDirty=function(t){var H=t;for(t=t.parentNode;t&&t!=document.body;){var w=jQuery.data(t,\"layout\");if(w){w.setElDirty(H);K.scheduleAdjust()}H=t;t=t.parentNode}};this.setChildLayoutsDirty=function(t,H){var w,z;w=0;for(z=t.descendants.length;w< z;++w){var A=t.descendants[w];if(H){var E=t.WT.getElement(A.getId());if(E&&!t.WT.contains(H,E))continue}A.setDirty()}};this.add=function(t){function H(w,z){var A,E;A=0;for(E=w.length;A<E;++A){var y=w[A];if(y.getId()==z.getId()){w[A]=z;z.descendants=y.descendants;return}else if(y.contains(z)){H(y.descendants,z);return}else if(z.contains(y)){z.descendants.push(y);w.splice(A,1);--A;--E}}w.push(z)}H(D,t);K.scheduleAdjust()};var Q=false,M=false;this.scheduleAdjust=function(t){if(t)P=true;if(!Q){Q=true; setTimeout(function(){K.adjust()},0)}};this.adjust=function(t,H){function w(A,E){var y,L;y=0;for(L=A.length;y<L;++y){var J=A[y];w(J.descendants,E);if(E==1&&P)J.setDirty();else E==0&&M&&J.setAllDirty();J.measure(E)}}function z(A,E){var y,L;y=0;for(L=A.length;y<L;++y){var J=A[y];if(J.apply(E))z(J.descendants,E);else{A.splice(y,1);--y;--L}}}if(t){(t=this.find(t))&&t.setItemsDirty(H);K.scheduleAdjust()}else{Q=false;if(!I){I=true;M=false;w(D,0);z(D,0);w(D,1);z(D,1);if(M){w(D,0);z(D,0);w(D,1);z(D,1)}P= M=I=false}}};this.updateConfig=function(t,H){(t=this.find(t))&&t.setConfig(H)};this.remeasure=function(){M=true};this.adjustNow=function(){Q&&K.adjust()};var V=null;window.onresize=function(){clearTimeout(V);V=setTimeout(function(){V=null;K.scheduleAdjust(true)},20)};window.onshow=function(){P=true;K.adjust()}})");
	}

	static WJavaScriptPreamble wtjs10() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptFunction,
				"ChildrenResize",
				"function(a,e,c,d){function i(j){var k=b.px(j,\"marginTop\");k+=b.px(j,\"marginBottom\");if(!b.boxSizing(j)){k+=b.px(j,\"borderTopWidth\");k+=b.px(j,\"borderBottomWidth\");k+=b.px(j,\"paddingTop\");k+=b.px(j,\"paddingBottom\")}return k}var b=this,h=c>=0;a.lh=h&&d;a.style.height=h?c+\"px\":\"\";if(b.boxSizing(a)){c-=b.px(a,\"marginTop\");c-=b.px(a,\"marginBottom\");c-=b.px(a,\"borderTopWidth\");c-=b.px(a,\"borderBottomWidth\");c-=b.px(a,\"paddingTop\");c-=b.px(a,\"paddingBottom\"); e-=b.px(a,\"marginLeft\");e-=b.px(a,\"marginRight\");e-=b.px(a,\"borderLeftWidth\");e-=b.px(a,\"borderRightWidth\");e-=b.px(a,\"paddingLeft\");e-=b.px(a,\"paddingRight\")}var g,m,f;g=0;for(m=a.childNodes.length;g<m;++g){f=a.childNodes[g];if(f.nodeType==1)if(h){var l=c-i(f);if(l>0)if(f.wtResize)f.wtResize(f,e,l,d);else{l=l+\"px\";if(f.style.height!=l){f.style.height=l;f.lh=d}}}else if(f.wtResize)f.wtResize(f,e,-1);else{f.style.height=\"\";f.lh=false}}}");
	}

	static WJavaScriptPreamble wtjs11() {
		return new WJavaScriptPreamble(JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptFunction, "ChildrenGetPS",
				"function(a,e,c,d){return d}");
	}

	static WJavaScriptPreamble wtjs12() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptFunction,
				"LastResize",
				"function(a,e,c,d){var i=this,b=c>=0;a.lh=b&&d;a.style.height=b?c+\"px\":\"\";a=a.lastChild;var h=a.previousSibling;if(b){c-=h.offsetHeight+i.px(h,\"marginTop\")+i.px(h,\"marginBottom\");if(c>0)if(a.wtResize)a.wtResize(a,e,c,d);else{a.style.height=c+\"px\";a.lh=d}}else if(a.wtResize)a.wtResize(a,-1,-1);else{a.style.height=\"\";a.lh=false}}");
	}

	static WJavaScriptPreamble wtjs13() {
		return new WJavaScriptPreamble(
				JavaScriptScope.WtClassScope,
				JavaScriptObjectType.JavaScriptFunction,
				"LastGetPS",
				"function(a,e,c,d){var i=this,b,h;if(e.wtGetPS)return d;b=0;for(h=a.childNodes.length;b<h;++b){var g=a.childNodes[b];if(g!=e){var m=i.css(g,\"position\");if(m!=\"absolute\"&&m!=\"fixed\")if(c===0)d=Math.max(d,g.offsetWidth);else d+=g.offsetHeight+i.px(g,\"marginTop\")+i.px(g,\"marginBottom\")}}return d}");
	}
}
