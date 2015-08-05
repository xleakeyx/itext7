package com.itextpdf.model.renderer;

import com.itextpdf.model.Property;
import com.itextpdf.model.element.Cell;
import com.itextpdf.model.layout.LayoutContext;
import com.itextpdf.model.layout.LayoutResult;

public class CellRenderer extends BlockRenderer {

    public CellRenderer(Cell modelElement) {
        super(modelElement);
        setProperty(Property.ROWSPAN, modelElement.getRowspan());
        setProperty(Property.COLSPAN, modelElement.getColspan());
    }

    @Override
    public Cell getModelElement() {
        return (Cell) super.getModelElement();
    }

    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
        return super.layout(layoutContext);
    }

    @Override
    protected CellRenderer createSplitRenderer(int layoutResult) {
        CellRenderer splitRenderer = new CellRenderer(getModelElement());
        splitRenderer.parent = parent;
        splitRenderer.modelElement = modelElement;
        splitRenderer.occupiedArea = occupiedArea;
        splitRenderer.addAllProperties(getOwnProperties());
        return splitRenderer;
    }

    @Override
    protected BlockRenderer createOverflowRenderer(int layoutResult) {
        CellRenderer overflowRenderer = new CellRenderer(getModelElement());
        overflowRenderer.parent = parent;
        overflowRenderer.modelElement = modelElement;
        overflowRenderer.addAllProperties(getOwnProperties());
        return overflowRenderer;
    }
}