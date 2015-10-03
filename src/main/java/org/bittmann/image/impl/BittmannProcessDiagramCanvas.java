package org.bittmann.image.impl;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.awt.BasicStroke;
import java.awt.Color;

import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.image.impl.DefaultProcessDiagramCanvas;

public class BittmannProcessDiagramCanvas extends DefaultProcessDiagramCanvas {

	protected static Color LABEL_COLOR = new Color(0, 0, 0);
	protected static Stroke MESSAGE_FLOW_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 10.0f,  new float[] { 5.0f }, 0.0f);
	
	public BittmannProcessDiagramCanvas(int width, int height, int minX, int minY, String imageType) {
		super(width, height, minX, minY, imageType);
	}

	public BittmannProcessDiagramCanvas(int i, int j, int minX, int minY, String imageType, String activityFontName,
			String labelFontName, ClassLoader customClassLoader) {
		super(i,j,minX,minY,imageType,activityFontName,
			labelFontName, customClassLoader);
	}
	
	public void drawLabel(String text, GraphicInfo graphicInfo, boolean centered){
		float interline = 1.0f;
		
	    // text
	    if (text != null && text.length()>0) {
	      Paint originalPaint = g.getPaint();
	      Font originalFont = g.getFont();

	      g.setPaint(LABEL_COLOR);
	      //g.setFont(LABEL_FONT);

	      int wrapWidth = 100;//(int) graphicInfo.getWidth();
	      int textY = (int) (graphicInfo.getY()+ (graphicInfo.getHeight()+5));
	      
	      // TODO: use drawMultilineText()
	      AttributedString as = new AttributedString(text);
	      as.addAttribute(TextAttribute.FOREGROUND, g.getPaint());
	      as.addAttribute(TextAttribute.FONT, g.getFont());
	      AttributedCharacterIterator aci = as.getIterator();
	      FontRenderContext frc = new FontRenderContext(null, true, false);
	      LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
	      
	      while (lbm.getPosition() < text.length()) {
	    	  TextLayout tl = lbm.nextLayout(wrapWidth);
	    	  textY += tl.getAscent();
	    	  Rectangle2D bb = tl.getBounds();
	    	  double tX = graphicInfo.getX();
	    	  if (centered) {
	    	  	tX += (int) (graphicInfo.getWidth() / 2 - bb.getWidth() / 2);
	    	  }
	    	  tl.draw(g, (float) tX, textY);
	    	  textY += tl.getDescent() + tl.getLeading() + (interline - 1.0f) * tl.getAscent();
	      }
	      
	      // restore originals
	      g.setFont(originalFont);
	      g.setPaint(originalPaint);
	    }
	  }
	
	public void drawPoolOrLane(String name, GraphicInfo graphicInfo) {
	    
		int x = (int) graphicInfo.getX();
	    int y = (int) graphicInfo.getY();
	    int width = (int) graphicInfo.getWidth();
	    int height = (int) graphicInfo.getHeight();
	    g.drawRect(x, y, width, height);
	    
	    // Add the name as text, vertical
	    if(name != null && name.length() > 0) {
	      // Include some padding
	      int availableTextSpace = height - 6;

	      // Create rotation for derived font
	      AffineTransform transformation = new AffineTransform();
	      //transformation.setToIdentity();
	      //transformation.rotate(270 * Math.PI/180);
	      transformation.rotate(90 * java.lang.Math.PI/180);
	      
	      Font currentFont = g.getFont();
	      AffineTransform fontAT = new AffineTransform();                
          fontAT.rotate(-Math.PI / 2);
          Font theDerivedFont = currentFont.deriveFont(transformation);
	      g.setFont(theDerivedFont);
          String truncated = fitTextToWidth(name, availableTextSpace);
	      int realWidth = fontMetrics.stringWidth(truncated);
	      
	      g.setFont(currentFont);
	      g.rotate(-Math.PI / 2, x, y);
	      g.drawString(truncated, x - ((height/2)+(realWidth/2)),y+fontMetrics.getHeight());
	      g.rotate(Math.PI / 2, x, y);
	    }
	  }
	
	protected void drawTask(String name, GraphicInfo graphicInfo, boolean thickBorder) {
	    Paint originalPaint = g.getPaint();
	    int x = (int) graphicInfo.getX();
	    int y = (int) graphicInfo.getY();
	    int width = (int) graphicInfo.getWidth();
	    int height = (int) graphicInfo.getHeight();
	    
	    // Create a new gradient paint for every task box, gradient depends on x and y and is not relative
	    g.setPaint(TASK_BOX_COLOR);

	    int arcR = 6;
	    if (thickBorder)
	    	arcR = 3;
	    
	    // shape
	    RoundRectangle2D rect = new RoundRectangle2D.Double(x, y, width, height, arcR, arcR);
	    g.fill(rect);
	    g.setPaint(TASK_BORDER_COLOR);

	    if (thickBorder) {
	      Stroke originalStroke = g.getStroke();
	      g.setStroke(THICK_TASK_BORDER_STROKE);
	      g.draw(rect);
	      g.setStroke(originalStroke);
	    } else {
	      g.draw(rect);
	    }

	    g.setPaint(originalPaint);
	    // text
	    if (name != null && name.length() > 0) {
	      
	      int boxWidth = width - (2 * (TEXT_PADDING+ICON_PADDING));
	      int boxHeight = height - ICON_PADDING - 2 - 2;
	      int boxX = x + width/2 - boxWidth/2;
	      int boxY = y + height/2 - boxHeight/2 + ICON_PADDING - 2 - 2;
	      drawMultilineCentredText(name, boxX, boxY, boxWidth, boxHeight);
	    }
	  }
	
	
	public void drawMessageflow(int[] xPoints, int[] yPoints, boolean isDefault, double scaleFactor) {
		Paint originalPaint = g.getPaint();
	    Stroke originalStroke = g.getStroke();

	    g.setPaint(CONNECTION_COLOR);
	    g.setStroke(MESSAGE_FLOW_STROKE);
	    
	    for (int i=1; i<xPoints.length; i++) {
	      Integer sourceX = xPoints[i - 1];
	      Integer sourceY = yPoints[i - 1];
	      Integer targetX = xPoints[i];
	      Integer targetY = yPoints[i];
	      Line2D.Double line = new Line2D.Double(sourceX, sourceY, targetX, targetY);
	      g.draw(line);
	    }
	  
	    
	    Line2D.Double line_s = new Line2D.Double(xPoints[xPoints.length-2], yPoints[xPoints.length-2], xPoints[xPoints.length-1], yPoints[xPoints.length-1]);
	    drawHollowArrowHead(line_s, scaleFactor);
	    Line2D.Double line_e = new Line2D.Double(xPoints[1], yPoints[1], xPoints[0], yPoints[0]);
	    drawHollowCircleHead(line_e,scaleFactor);
	    
	    g.setPaint(originalPaint);
	    g.setStroke(originalStroke);
		
	}
	
	public void drawHollowCircleHead(Line2D.Double line,double scaleFactor){
		AffineTransform transformation = getTransformation(line);
		Ellipse2D.Double circleHead = new Ellipse2D.Double(-2.5f, -2.5f, 5, 5);
	    
	    AffineTransform originalTransformation = g.getTransform();
	    Color originalColor = g.getColor();
	    Stroke originalStroke = g.getStroke();
	    g.setTransform(transformation);
	    g.setStroke(new BasicStroke(1.0f));
	    
	    g.setColor(Color.white);
	    g.fill(circleHead);
	    
	    g.setColor(Color.black);
	    g.draw(circleHead);
	    
	    g.setTransform(originalTransformation);
	    g.setColor(originalColor);
	    g.setStroke(originalStroke);
	}

	private AffineTransform getTransformation(Line2D.Double line) {
		AffineTransform transformation = new AffineTransform();
	    transformation.setToIdentity();
	    double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
	    transformation.translate(line.x2, line.y2);
	    transformation.rotate((angle - Math.PI / 2d));
		return transformation;
	}
	
	public void drawHollowArrowHead(Line2D.Double line, double scaleFactor) {
	    
		int doubleArrowWidth = (int) (2 * ARROW_WIDTH / scaleFactor);
	    if (doubleArrowWidth == 0) {
	      doubleArrowWidth = 2;
	    }
	    Polygon arrowHead = new Polygon();
	    arrowHead.addPoint(0, 0);
	    int arrowHeadPoint = (int) (-ARROW_WIDTH / scaleFactor);
	    if (arrowHeadPoint == 0) {
	      arrowHeadPoint = -1;
	    }
	    arrowHead.addPoint(arrowHeadPoint, -doubleArrowWidth);
	    arrowHeadPoint = (int) (ARROW_WIDTH / scaleFactor);
	    if (arrowHeadPoint == 0) {
	      arrowHeadPoint = 1;
	    }
	    arrowHead.addPoint(arrowHeadPoint, -doubleArrowWidth);

	    AffineTransform transformation = getTransformation(line);

	    AffineTransform originalTransformation = g.getTransform();
	    Color originalColor = g.getColor();
	    Stroke originalStroke = g.getStroke();
	    g.setTransform(transformation);
	    g.setStroke(ANNOTATION_STROKE);
	    
	    g.setColor(Color.white);
	    g.fill(arrowHead);
	    
	    g.setColor(Color.black);
	    g.draw(arrowHead);
	    
	    g.setTransform(originalTransformation);
	    g.setColor(originalColor);
	    g.setStroke(originalStroke);
	  }

	public void drawExclusiveGateway(GraphicInfo graphicInfo, double scaleFactor) {
		super.drawExclusiveGateway(graphicInfo, scaleFactor);
		
		if(graphicInfo.getElement().getName() != null){
			drawLabel(graphicInfo.getElement().getName(), graphicInfo);	
		}
		
	}
	public void drawInclusiveGateway(GraphicInfo graphicInfo, double scaleFactor) {
		super.drawInclusiveGateway(graphicInfo, scaleFactor);
		
		drawLabel(graphicInfo.getElement().getName(), graphicInfo);
	}
	public void drawParallelGateway(GraphicInfo graphicInfo, double scaleFactor) {
		super.drawParallelGateway(graphicInfo, scaleFactor);
		drawLabel(graphicInfo.getElement().getName(), graphicInfo);
	}
	
	public void drawDataObject(String name,GraphicInfo graphicInfo, double scaleFactor){
		Polygon dataObjectPoly = new Polygon();
	    //x = 250 - 175 = 75 
		//y = 395 -305 = 95
		dataObjectPoly.addPoint(75, -75);
		dataObjectPoly.addPoint(50, -100);
		dataObjectPoly.addPoint(0, -100);
		dataObjectPoly.addPoint(0, 0);
		dataObjectPoly.addPoint(75, 0);
		dataObjectPoly.addPoint(75, -75);
		dataObjectPoly.addPoint(50, -75);
		dataObjectPoly.addPoint(50, -100);
	    g.setColor(Color.BLACK);
	    g.fill(dataObjectPoly);
	    
		drawLabel(name, graphicInfo);
		
	}

	public void drawDataStore(String name, GraphicInfo graphicInfo, float f) {
		Polygon dataObjectPoly = new Polygon();
	    //x = 250 - 175 = 75 
		//y = 395 -305 = 95
		dataObjectPoly.addPoint(75, -75);
		dataObjectPoly.addPoint(50, -100);
		dataObjectPoly.addPoint(0, -100);
		dataObjectPoly.addPoint(0, 0);
		dataObjectPoly.addPoint(75, 0);
		dataObjectPoly.addPoint(75, -75);
		dataObjectPoly.addPoint(50, -75);
		dataObjectPoly.addPoint(50, -100);
	    g.setColor(Color.BLACK);
	    g.fill(dataObjectPoly);
	    
		drawLabel(name, graphicInfo);
		
	}
}
