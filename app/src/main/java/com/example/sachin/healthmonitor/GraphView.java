package com.example.sachin.healthmonitor;//add your own package name
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

	public static boolean BAR = false;
	public static boolean LINE = true;

	private Paint paint;
	private float[] xvalues;
	private float[] yvalues;
	private float[] zvalues;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean type;

	public GraphView(Context context, float[] xvalues, float[] yvalues, float[] zvalues, String title, String[] horlabels, String[] verlabels, boolean type) {
		super(context);
		if (xvalues == null)
			xvalues = new float[0];
		else
			this.xvalues = xvalues;
		if (yvalues == null)
			yvalues = new float[0];
		else
			this.yvalues = yvalues;
		if (zvalues == null)
			zvalues = new float[0];
		else
			this.zvalues = zvalues;
		if (title == null)
			title = "";
		else
			this.title = title;
		if (horlabels == null)
			this.horlabels = new String[0];
		else
			this.horlabels = horlabels;
		if (verlabels == null)
			this.verlabels = new String[0];
		else
			this.verlabels = verlabels;
		this.type = type;
		paint = new Paint();
	}

	public void setxValues(float[] newValues)
	{
		this.xvalues = newValues;
	}
	public void setyValues(float[] newValues)
	{
		this.yvalues = newValues;
	}
	public void setzValues(float[] newValues)
	{
		this.zvalues = newValues;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float border = 30;
		float horstart = border * 2;
		float height = getHeight() - 1;
		float width = getWidth() - 1;
		float max = getMax();
		float min = getMin();
		float diff = max - min;
		float graphheight = height - (2 * border);
		float graphwidth = width - (2 * border) - 50;


		paint.setTextAlign(Align.LEFT);
		int vers = verlabels.length - 1;
		for (int i = 0; i < verlabels.length; i++) {
			paint.setColor(Color.BLACK);
			float y = ((graphheight / vers) * i) + border;
			canvas.drawLine(horstart, y, width - horstart + 10, y, paint);
			paint.setColor(Color.BLACK);
			paint.setTextSize(35);
			canvas.drawText(verlabels[i], 0, y + 10, paint);
		}
		int hors = horlabels.length - 1;
		for (int i = 0; i < horlabels.length; i++) {
			paint.setColor(Color.BLACK);
			float x = ((graphwidth / hors) * i) + horstart;
			canvas.drawLine(x, height - border, x, border, paint);
			if (i==horlabels.length-1)
				paint.setTextAlign(Align.RIGHT);
			if (i==0)
				paint.setTextAlign(Align.LEFT);
			paint.setColor(Color.BLACK);
			paint.setTextSize(35);
			canvas.drawText(horlabels[i], x, height - 4, paint);
		}

		paint.setTextAlign(Align.CENTER);
        paint.setTextSize(40);
		canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);
			paint.setColor(Color.BLACK);
			float datalength = xvalues.length;
			float colwidth = (width - (2 * border)) / datalength;
			float halfcol = colwidth / 2;
			float lasth = 0;
		float lasthy = 0;
		float lasthz = 0;
		for (int i = 0; i < xvalues.length; i++) {
			float h;
			if (xvalues[i] > 0) {
				float m = new Float(verlabels[0]) * 2;
				float p = ((xvalues[i] * 2) / m) * 100;
				h = (graphheight * p) / 100;
			} else if (xvalues[i] < 0) {
				float m = new Float(verlabels[0]);
				float p = ((xvalues[i]) / m) * 100;
				h = (graphheight * p) / 100;
				h += graphheight;
			} else {
				h = graphheight / 2;
			}

			float hy;
			if (yvalues[i] > 0) {
				float my = new Float(verlabels[0]) * 2;
				float py = ((yvalues[i] * 2) / my) * 100;
				hy = (graphheight * py) / 100;
			} else if (yvalues[i] < 0) {
				float my = new Float(verlabels[0]);
				float py = ((yvalues[i]) / my) * 100;
				hy = (graphheight * py) / 100;
				hy += graphheight;
			} else {
				hy = graphheight / 2;
			}

			float hz;
			if (zvalues[i] > 0) {
				float mz = new Float(verlabels[0]) * 2;
				float pz = ((zvalues[i] * 2) / mz) * 100;
				hz = (graphheight * pz) / 100;
			} else if (zvalues[i] < 0) {
				float mz = new Float(verlabels[0]);
				float pz = ((zvalues[i]) / mz) * 100;
				hz = (graphheight * pz) / 100;
				hz += graphheight;
			} else {
				hz = graphheight / 2;
			}
			if (i > 0) {
				paint.setColor(Color.BLUE);
				paint.setStrokeWidth(8.0f);
				canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
				paint.setColor(Color.GREEN);
				canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasthy) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - hy) + graphheight, paint);
				paint.setColor(Color.RED);
				canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasthz) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - hz) + graphheight, paint);
				paint.setStrokeWidth(4.0f);
			}
				lasth = h;
				lasthy = hy;
			lasthz = hz;
		}
	}

	private float getMax() {
		float largest = Integer.MIN_VALUE;
		for (int i = 0; i < xvalues.length; i++)
			if (xvalues[i] > largest)
				largest = xvalues[i];

		//largest = 3000;
		return largest;
	}

	private float getMin() {
		float smallest = Integer.MAX_VALUE;
		for (int i = 0; i < xvalues.length; i++)
			if (xvalues[i] < smallest)
				smallest = xvalues[i];

		//smallest = 0;
		return smallest;
	}

}