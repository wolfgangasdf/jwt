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

/**
 * A layout manager which arranges widgets horizontally or vertically.
 * <p>
 * 
 * This layout manager arranges widgets horizontally or vertically inside the
 * parent container.
 * <p>
 * The space is divided so that each widget is given its preferred size, and
 * remaining space is divided according to stretch factors among widgets. If not
 * all widgets can be given their preferred size (there is not enough room),
 * then widgets are given a smaller size (down to their minimum size). If
 * necessary, the container (or parent layout) of this layout is resized to meet
 * minimum size requirements.
 * <p>
 * The preferred width or height of a widget is based on its natural size, where
 * it presents its contents without overflowing.
 * {@link WWidget#resize(WLength width, WLength height) WWidget#resize()} or
 * (CSS <code>width</code>, <code>height</code> properties) can be used to
 * adjust the preferred size of a widget.
 * <p>
 * The minimum width or height of a widget is based on the minimum dimensions of
 * the widget or the nested layout. The default minimum height or width for a
 * widget is 0. It can be specified using
 * {@link WWidget#setMinimumSize(WLength width, WLength height)
 * WWidget#setMinimumSize()} or using CSS <code>min-width</code> or
 * <code>min-height</code> properties.
 * <p>
 * You should use
 * {@link WContainerWidget#setOverflow(WContainerWidget.Overflow value, EnumSet orientation)
 * WContainerWidget::setOverflow(OverflowAuto)} or use a {@link WScrollArea} to
 * automatically show scrollbars for widgets inserted in the layout to cope with
 * a size set by the layout manager that is smaller than the preferred size.
 * <p>
 * When the container of a layout manager has a maximum size set using
 * {@link WWidget#setMaximumSize(WLength width, WLength height)
 * WWidget#setMaximumSize()}, then the size of the container will be based on
 * the preferred size of the contents, up to this maximum size, instead of the
 * default behaviour of constraining the size of the children based on the size
 * of the container.
 * <p>
 * A layout manager may provide resize handles between items which allow the
 * user to change the automatic layout provided by the layout manager (see
 * {@link WBoxLayout#setResizable(int index, boolean enabled, WLength initialSize)
 * setResizable()}).
 * <p>
 * Each item is separated using a constant spacing, which defaults to 6 pixels,
 * and can be changed using {@link WBoxLayout#setSpacing(int size) setSpacing()}
 * . In addition, when this layout is a top-level layout (i.e. is not nested
 * inside another layout), a margin is set around the contents. This margin
 * defaults to 9 pixels, and can be changed using
 * {@link WLayout#setContentsMargins(int left, int top, int right, int bottom)
 * WLayout#setContentsMargins()}. You can add more space between two widgets
 * using {@link WBoxLayout#addSpacing(WLength size) addSpacing()}.
 * <p>
 * For each item a stretch factor may be defined, which controls how remaining
 * space is used. Each item is stretched using the stretch factor to fill the
 * remaining space.
 * <p>
 * <p>
 * <i><b>Note: </b>When JavaScript support is not available, not all
 * functionality of the layout is available. In particular, vertical size
 * management is not available.
 * <p>
 * When a layout is used on a first page with progressive bootstrap, then the
 * layout will progress only in a limited way to a full JavaScript-based layout.
 * You can thus not rely on it to behave properly for example when dynamically
 * adding or removing widgets. </i>
 * </p>
 */
public class WBoxLayout extends WLayout {
	private static Logger logger = LoggerFactory.getLogger(WBoxLayout.class);

	/**
	 * Enumeration of the direction in which widgets are layed out.
	 */
	public enum Direction {
		/**
		 * Horizontal layout, widgets are arranged from left to right.
		 */
		LeftToRight,
		/**
		 * Horizontal layout, widgets are arranged from right to left.
		 */
		RightToLeft,
		/**
		 * Vertical layout, widgets are arranged from top to bottom.
		 */
		TopToBottom,
		/**
		 * Vertical layout, widgets are arranged from bottom to top.
		 */
		BottomToTop;

		/**
		 * Returns the numerical representation of this enum.
		 */
		public int getValue() {
			return ordinal();
		}
	}

	/**
	 * Creates a new box layout.
	 * <p>
	 * This constructor is rarely used. Instead, use the convenient constructors
	 * of the specialized {@link WHBoxLayout} or {@link WVBoxLayout} classes.
	 * <p>
	 * Use <code>parent</code> = <code>null</code> to created a layout manager
	 * that can be nested inside other layout managers.
	 */
	public WBoxLayout(WBoxLayout.Direction dir, WWidget parent) {
		super();
		this.direction_ = dir;
		this.grid_ = new Grid();
		if (parent != null) {
			this.setLayoutInParent(parent);
		}
	}

	/**
	 * Creates a new box layout.
	 * <p>
	 * Calls {@link #WBoxLayout(WBoxLayout.Direction dir, WWidget parent)
	 * this(dir, (WWidget)null)}
	 */
	public WBoxLayout(WBoxLayout.Direction dir) {
		this(dir, (WWidget) null);
	}

	public void addItem(WLayoutItem item) {
		this.insertItem(this.getCount(), item, 0, EnumSet
				.noneOf(AlignmentFlag.class));
	}

	public void removeItem(WLayoutItem item) {
		int index = this.indexOf(item);
		if (index != -1) {
			switch (this.direction_) {
			case RightToLeft:
				index = this.grid_.columns_.size() - 1 - index;
			case LeftToRight:
				this.grid_.columns_.remove(0 + index);
				this.grid_.items_.get(0).remove(0 + index);
				break;
			case BottomToTop:
				index = this.grid_.rows_.size() - 1 - index;
			case TopToBottom:
				this.grid_.rows_.remove(0 + index);
				this.grid_.items_.remove(0 + index);
			}
			this.updateRemoveItem(item);
		}
	}

	public WLayoutItem getItemAt(int index) {
		switch (this.direction_) {
		case RightToLeft:
			index = this.grid_.columns_.size() - 1 - index;
		case LeftToRight:
			return this.grid_.items_.get(0).get(index).item_;
		case BottomToTop:
			index = this.grid_.rows_.size() - 1 - index;
		case TopToBottom:
			return this.grid_.items_.get(index).get(0).item_;
		}
		assert false;
		return null;
	}

	public int getCount() {
		return this.grid_.rows_.size() * this.grid_.columns_.size();
	}

	public void clear() {
		while (this.getCount() != 0) {
			WLayoutItem item = this.getItemAt(this.getCount() - 1);
			this.clearLayoutItem(item);
		}
	}

	/**
	 * Sets the layout direction.
	 * <p>
	 * 
	 * @see WBoxLayout#getDirection()
	 */
	public void setDirection(WBoxLayout.Direction direction) {
		if (this.direction_ != direction) {
			this.direction_ = direction;
		}
	}

	/**
	 * Returns the layout direction.
	 * <p>
	 * 
	 * @see WBoxLayout#setDirection(WBoxLayout.Direction direction)
	 */
	public WBoxLayout.Direction getDirection() {
		return this.direction_;
	}

	/**
	 * Sets spacing between each item.
	 * <p>
	 * The default spacing is 6 pixels.
	 */
	public void setSpacing(int size) {
		this.grid_.horizontalSpacing_ = size;
		this.grid_.verticalSpacing_ = size;
	}

	/**
	 * Returns the spacing between each item.
	 * <p>
	 * 
	 * @see WBoxLayout#setSpacing(int size)
	 */
	public int getSpacing() {
		return this.grid_.horizontalSpacing_;
	}

	/**
	 * Adds a widget to the layout.
	 * <p>
	 * Adds a widget to the layout, with given <code>stretch</code> factor. When
	 * the stretch factor is 0, the widget will not be resized by the layout
	 * manager (stretched to take excess space).
	 * <p>
	 * The <code>alignment</code> parameter is a combination of a horizontal
	 * and/or a vertical AlignmentFlag OR&apos;ed together.
	 * <p>
	 * The <code>alignment</code> specifies the vertical and horizontal
	 * alignment of the item. The default value 0 indicates that the item is
	 * stretched to fill the entire column or row. The alignment can be
	 * specified as a logical combination of a horizontal alignment (
	 * {@link AlignmentFlag#AlignLeft}, {@link AlignmentFlag#AlignCenter}, or
	 * {@link AlignmentFlag#AlignRight}) and a vertical alignment (
	 * {@link AlignmentFlag#AlignTop}, {@link AlignmentFlag#AlignMiddle}, or
	 * {@link AlignmentFlag#AlignBottom}).
	 * <p>
	 * 
	 * @see WBoxLayout#addLayout(WLayout layout, int stretch, EnumSet alignment)
	 * @see WBoxLayout#insertWidget(int index, WWidget widget, int stretch,
	 *      EnumSet alignment)
	 */
	public void addWidget(WWidget widget, int stretch,
			EnumSet<AlignmentFlag> alignment) {
		this.insertWidget(this.getCount(), widget, stretch, alignment);
	}

	/**
	 * Adds a widget to the layout.
	 * <p>
	 * Calls {@link #addWidget(WWidget widget, int stretch, EnumSet alignment)
	 * addWidget(widget, stretch, EnumSet.of(alignmen, alignment))}
	 */
	public final void addWidget(WWidget widget, int stretch,
			AlignmentFlag alignmen, AlignmentFlag... alignment) {
		addWidget(widget, stretch, EnumSet.of(alignmen, alignment));
	}

	/**
	 * Adds a widget to the layout.
	 * <p>
	 * Calls {@link #addWidget(WWidget widget, int stretch, EnumSet alignment)
	 * addWidget(widget, 0, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void addWidget(WWidget widget) {
		addWidget(widget, 0, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Adds a widget to the layout.
	 * <p>
	 * Calls {@link #addWidget(WWidget widget, int stretch, EnumSet alignment)
	 * addWidget(widget, stretch, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void addWidget(WWidget widget, int stretch) {
		addWidget(widget, stretch, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Adds a nested layout to the layout.
	 * <p>
	 * Adds a nested layout, with given <code>stretch</code> factor.
	 * <p>
	 * 
	 * @see WBoxLayout#addWidget(WWidget widget, int stretch, EnumSet alignment)
	 * @see WBoxLayout#insertLayout(int index, WLayout layout, int stretch,
	 *      EnumSet alignment)
	 */
	public void addLayout(WLayout layout, int stretch,
			EnumSet<AlignmentFlag> alignment) {
		this.insertLayout(this.getCount(), layout, stretch, alignment);
	}

	/**
	 * Adds a nested layout to the layout.
	 * <p>
	 * Calls {@link #addLayout(WLayout layout, int stretch, EnumSet alignment)
	 * addLayout(layout, stretch, EnumSet.of(alignmen, alignment))}
	 */
	public final void addLayout(WLayout layout, int stretch,
			AlignmentFlag alignmen, AlignmentFlag... alignment) {
		addLayout(layout, stretch, EnumSet.of(alignmen, alignment));
	}

	/**
	 * Adds a nested layout to the layout.
	 * <p>
	 * Calls {@link #addLayout(WLayout layout, int stretch, EnumSet alignment)
	 * addLayout(layout, 0, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void addLayout(WLayout layout) {
		addLayout(layout, 0, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Adds a nested layout to the layout.
	 * <p>
	 * Calls {@link #addLayout(WLayout layout, int stretch, EnumSet alignment)
	 * addLayout(layout, stretch, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void addLayout(WLayout layout, int stretch) {
		addLayout(layout, stretch, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Adds extra spacing.
	 * <p>
	 * Adds extra spacing to the layout.
	 * <p>
	 * 
	 * @see WBoxLayout#addStretch(int stretch)
	 * @see WBoxLayout#insertStretch(int index, int stretch)
	 */
	public void addSpacing(final WLength size) {
		this.insertSpacing(this.getCount(), size);
	}

	/**
	 * Adds a stretch element.
	 * <p>
	 * Adds a stretch element to the layout. This adds an empty space that
	 * stretches as needed.
	 * <p>
	 * 
	 * @see WBoxLayout#addSpacing(WLength size)
	 * @see WBoxLayout#insertStretch(int index, int stretch)
	 */
	public void addStretch(int stretch) {
		this.insertStretch(this.getCount(), stretch);
	}

	/**
	 * Adds a stretch element.
	 * <p>
	 * Calls {@link #addStretch(int stretch) addStretch(0)}
	 */
	public final void addStretch() {
		addStretch(0);
	}

	/**
	 * Inserts a widget in the layout.
	 * <p>
	 * Inserts a widget in the layout at position <code>index</code>, with given
	 * <code>stretch</code> factor. When the stretch factor is 0, the widget
	 * will not be resized by the layout manager (stretched to take excess
	 * space).
	 * <p>
	 * The <code>alignment</code> specifies the vertical and horizontal
	 * alignment of the item. The default value 0 indicates that the item is
	 * stretched to fill the entire column or row. The alignment can be
	 * specified as a logical combination of a horizontal alignment (
	 * {@link AlignmentFlag#AlignLeft}, {@link AlignmentFlag#AlignCenter}, or
	 * {@link AlignmentFlag#AlignRight}) and a vertical alignment (
	 * {@link AlignmentFlag#AlignTop}, {@link AlignmentFlag#AlignMiddle}, or
	 * {@link AlignmentFlag#AlignBottom}).
	 * <p>
	 * 
	 * @see WBoxLayout#insertLayout(int index, WLayout layout, int stretch,
	 *      EnumSet alignment)
	 * @see WBoxLayout#addWidget(WWidget widget, int stretch, EnumSet alignment)
	 */
	public void insertWidget(int index, WWidget widget, int stretch,
			EnumSet<AlignmentFlag> alignment) {
		if (widget.isLayoutSizeAware() && stretch == 0) {
			stretch = -1;
		}
		this.insertItem(index, new WWidgetItem(widget), stretch, alignment);
	}

	/**
	 * Inserts a widget in the layout.
	 * <p>
	 * Calls
	 * {@link #insertWidget(int index, WWidget widget, int stretch, EnumSet alignment)
	 * insertWidget(index, widget, stretch, EnumSet.of(alignmen, alignment))}
	 */
	public final void insertWidget(int index, WWidget widget, int stretch,
			AlignmentFlag alignmen, AlignmentFlag... alignment) {
		insertWidget(index, widget, stretch, EnumSet.of(alignmen, alignment));
	}

	/**
	 * Inserts a widget in the layout.
	 * <p>
	 * Calls
	 * {@link #insertWidget(int index, WWidget widget, int stretch, EnumSet alignment)
	 * insertWidget(index, widget, 0, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void insertWidget(int index, WWidget widget) {
		insertWidget(index, widget, 0, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts a widget in the layout.
	 * <p>
	 * Calls
	 * {@link #insertWidget(int index, WWidget widget, int stretch, EnumSet alignment)
	 * insertWidget(index, widget, stretch,
	 * EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void insertWidget(int index, WWidget widget, int stretch) {
		insertWidget(index, widget, stretch, EnumSet
				.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts a nested layout in the layout.
	 * <p>
	 * Inserts a nested layout in the layout at position<code>index</code>, with
	 * given <code>stretch</code> factor.
	 * <p>
	 * 
	 * @see WBoxLayout#insertWidget(int index, WWidget widget, int stretch,
	 *      EnumSet alignment)
	 * @see WBoxLayout#addLayout(WLayout layout, int stretch, EnumSet alignment)
	 */
	public void insertLayout(int index, WLayout layout, int stretch,
			EnumSet<AlignmentFlag> alignment) {
		this.insertItem(index, layout, stretch, alignment);
	}

	/**
	 * Inserts a nested layout in the layout.
	 * <p>
	 * Calls
	 * {@link #insertLayout(int index, WLayout layout, int stretch, EnumSet alignment)
	 * insertLayout(index, layout, stretch, EnumSet.of(alignmen, alignment))}
	 */
	public final void insertLayout(int index, WLayout layout, int stretch,
			AlignmentFlag alignmen, AlignmentFlag... alignment) {
		insertLayout(index, layout, stretch, EnumSet.of(alignmen, alignment));
	}

	/**
	 * Inserts a nested layout in the layout.
	 * <p>
	 * Calls
	 * {@link #insertLayout(int index, WLayout layout, int stretch, EnumSet alignment)
	 * insertLayout(index, layout, 0, EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void insertLayout(int index, WLayout layout) {
		insertLayout(index, layout, 0, EnumSet.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts a nested layout in the layout.
	 * <p>
	 * Calls
	 * {@link #insertLayout(int index, WLayout layout, int stretch, EnumSet alignment)
	 * insertLayout(index, layout, stretch,
	 * EnumSet.noneOf(AlignmentFlag.class))}
	 */
	public final void insertLayout(int index, WLayout layout, int stretch) {
		insertLayout(index, layout, stretch, EnumSet
				.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts extra spacing in the layout.
	 * <p>
	 * Inserts extra spacing in the layout at position <code>index</code>.
	 * <p>
	 * 
	 * @see WBoxLayout#insertStretch(int index, int stretch)
	 * @see WBoxLayout#addSpacing(WLength size)
	 */
	public void insertSpacing(int index, final WLength size) {
		WWidget spacer = this.createSpacer(size);
		this.insertItem(index, new WWidgetItem(spacer), 0, EnumSet
				.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts a stretch element in the layout.
	 * <p>
	 * Inserts a stretch element in the layout at position <code>index</code>.
	 * This adds an empty space that stretches as needed.
	 * <p>
	 * 
	 * @see WBoxLayout#insertSpacing(int index, WLength size)
	 * @see WBoxLayout#addStretch(int stretch)
	 */
	public void insertStretch(int index, int stretch) {
		WWidget spacer = this.createSpacer(new WLength(0));
		this.insertItem(index, new WWidgetItem(spacer), stretch, EnumSet
				.noneOf(AlignmentFlag.class));
	}

	/**
	 * Inserts a stretch element in the layout.
	 * <p>
	 * Calls {@link #insertStretch(int index, int stretch) insertStretch(index,
	 * 0)}
	 */
	public final void insertStretch(int index) {
		insertStretch(index, 0);
	}

	/**
	 * Sets the stretch factor for a nested layout.
	 * <p>
	 * The <code>layout</code> must have previously been added to this layout
	 * using
	 * {@link WBoxLayout#insertLayout(int index, WLayout layout, int stretch, EnumSet alignment)
	 * insertLayout()} or
	 * {@link WBoxLayout#addLayout(WLayout layout, int stretch, EnumSet alignment)
	 * addLayout()}.
	 * <p>
	 * Returns whether the <code>stretch</code> could be set.
	 */
	public boolean setStretchFactor(WLayout layout, int stretch) {
		for (int i = 0; i < this.getCount(); ++i) {
			WLayoutItem item = this.getItemAt(i);
			if (item != null && item.getLayout() == layout) {
				this.setStretchFactor(i, stretch);
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the stretch factor for a widget.
	 * <p>
	 * The <code>widget</code> must have previously been added to this layout
	 * using
	 * {@link WBoxLayout#insertWidget(int index, WWidget widget, int stretch, EnumSet alignment)
	 * insertWidget()} or
	 * {@link WBoxLayout#addWidget(WWidget widget, int stretch, EnumSet alignment)
	 * addWidget()}.
	 * <p>
	 * Returns whether the <code>stretch</code> could be set.
	 */
	public boolean setStretchFactor(WWidget widget, int stretch) {
		for (int i = 0; i < this.getCount(); ++i) {
			WLayoutItem item = this.getItemAt(i);
			if (item != null && item.getWidget() == widget) {
				this.setStretchFactor(i, stretch);
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets whether the use may drag a particular border.
	 * <p>
	 * This method sets whether the border that separates item <i>index</i> from
	 * the next item may be resized by the user, depending on the value of
	 * <i>enabled</i>.
	 * <p>
	 * The default value is <i>false</i>.
	 * <p>
	 * If an <code>initialSize</code> is given (that is not {@link WLength#Auto}
	 * ), then this size is used for the size of the item, overriding the size
	 * it would be given by the layout manager.
	 */
	public void setResizable(int index, boolean enabled,
			final WLength initialSize) {
		switch (this.direction_) {
		case RightToLeft:
			index = this.grid_.columns_.size() - 1 - index;
		case LeftToRight:
			this.grid_.columns_.get(index).resizable_ = enabled;
			this.grid_.columns_.get(index).initialSize_ = initialSize;
			break;
		case BottomToTop:
			index = this.grid_.rows_.size() - 1 - index;
		case TopToBottom:
			this.grid_.rows_.get(index).resizable_ = enabled;
			this.grid_.rows_.get(index).initialSize_ = initialSize;
		}
		this.update((WLayoutItem) null);
	}

	/**
	 * Sets whether the use may drag a particular border.
	 * <p>
	 * Calls {@link #setResizable(int index, boolean enabled, WLength initialSize)
	 * setResizable(index, true, WLength.Auto)}
	 */
	public final void setResizable(int index) {
		setResizable(index, true, WLength.Auto);
	}

	/**
	 * Sets whether the use may drag a particular border.
	 * <p>
	 * Calls {@link #setResizable(int index, boolean enabled, WLength initialSize)
	 * setResizable(index, enabled, WLength.Auto)}
	 */
	public final void setResizable(int index, boolean enabled) {
		setResizable(index, enabled, WLength.Auto);
	}

	/**
	 * Returns whether the user may drag a particular border.
	 * <p>
	 * This method returns whether the border that separates item <i>index</i>
	 * from the next item may be resized by the user.
	 * <p>
	 * 
	 * @see WBoxLayout#setResizable(int index, boolean enabled, WLength
	 *      initialSize)
	 */
	public boolean isResizable(int index) {
		switch (this.direction_) {
		case RightToLeft:
			index = this.grid_.columns_.size() - 1 - index;
		case LeftToRight:
			return this.grid_.columns_.get(index).resizable_;
		case BottomToTop:
			index = this.grid_.rows_.size() - 1 - index;
		case TopToBottom:
			return this.grid_.rows_.get(index).resizable_;
		}
		return false;
	}

	Grid getGrid() {
		return this.grid_;
	}

	protected void insertItem(int index, WLayoutItem item, int stretch,
			EnumSet<AlignmentFlag> alignment) {
		switch (this.direction_) {
		case RightToLeft:
			index = this.grid_.columns_.size() - index;
		case LeftToRight:
			this.grid_.columns_.add(0 + index, new Grid.Section(stretch));
			if (this.grid_.items_.isEmpty()) {
				this.grid_.items_.add(new ArrayList<Grid.Item>());
				this.grid_.rows_.add(new Grid.Section());
				this.grid_.rows_.get(0).stretch_ = -1;
			}
			this.grid_.items_.get(0).add(0 + index,
					new Grid.Item(item, alignment));
			break;
		case BottomToTop:
			index = this.grid_.rows_.size() - index;
		case TopToBottom:
			if (this.grid_.columns_.isEmpty()) {
				this.grid_.columns_.add(new Grid.Section());
				this.grid_.columns_.get(0).stretch_ = -1;
			}
			this.grid_.rows_.add(0 + index, new Grid.Section(stretch));
			this.grid_.items_.add(0 + index, new ArrayList<Grid.Item>());
			this.grid_.items_.get(index).add(new Grid.Item(item, alignment));
			break;
		}
		this.updateAddItem(item);
	}

	protected final void insertItem(int index, WLayoutItem item, int stretch,
			AlignmentFlag alignmen, AlignmentFlag... alignment) {
		insertItem(index, item, stretch, EnumSet.of(alignmen, alignment));
	}

	private WBoxLayout.Direction direction_;
	private Grid grid_;

	private void setStretchFactor(int i, int stretch) {
		switch (this.direction_) {
		case RightToLeft:
			i = this.grid_.columns_.size() - 1 - i;
		case LeftToRight:
			this.grid_.columns_.get(i).stretch_ = stretch;
			break;
		case BottomToTop:
			i = this.grid_.rows_.size() - 1 - i;
		case TopToBottom:
			this.grid_.rows_.get(i).stretch_ = stretch;
		}
	}

	private WWidget createSpacer(final WLength size) {
		Spacer spacer = new Spacer();
		if (size.toPixels() > 0) {
			if (this.direction_ == WBoxLayout.Direction.LeftToRight
					|| this.direction_ == WBoxLayout.Direction.RightToLeft) {
				spacer.setMinimumSize(size, WLength.Auto);
			} else {
				spacer.setMinimumSize(WLength.Auto, size);
			}
		}
		return spacer;
	}
}
