package com.riis.androidarduino.can;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

public class BreathalyzerView extends View {
	public static final float MAX = 950.0f;
	public static final float MIN = 225.0f;
	
	public static boolean BAR = true;
	public static boolean LINE = false;

	private Paint paint;
	private ArrayList<Float> values;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean type;

	public BreathalyzerView(Context context, ArrayList<Float> values, String title, String[] horlabels, String[] verlabels, boolean type) {
		super(context);
		
		if(values == null) {
			values = new ArrayList<Float>();
		} else {
			this.values = values;
		}
		
		if(title == null) {
			title = "";
		} else {
			this.title = title;
		}
		
		if(horlabels == null) {
			this.horlabels = new String[0];
		} else {
			this.horlabels = horlabels;
		}
		
		if(verlabels == null) {
			this.verlabels = new String[0];
		} else {
			this.verlabels = verlabels;
		}
		
		this.type = type;
		
		paint = new Paint();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float border = 20;
		float horstart = border * 2;
		float height = getHeight();
		float width = getWidth() - 1;
		float diff = MAX - MIN;
		float graphheight = height - (2 * border);
		float graphwidth = width - (2 * border);

		paint.setTextAlign(Align.LEFT);
		int vers = verlabels.length - 1;
		
		for (int i = 0; i < verlabels.length; i++) {
			paint.setColor(Color.DKGRAY);
			float y = ((graphheight / vers) * i) + border;
			canvas.drawLine(horstart, y, width, y, paint);
			paint.setColor(Color.WHITE);
			canvas.drawText(verlabels[i], 0, y, paint);
		}
		int hors = horlabels.length - 1;
		for (int i = 0; i < horlabels.length; i++) {
			paint.setColor(Color.DKGRAY);
			float x = ((graphwidth / hors) * i) + horstart;
			canvas.drawLine(x, height - border, x, border, paint);
			paint.setTextAlign(Align.CENTER);
			if (i==horlabels.length-1)
				paint.setTextAlign(Align.RIGHT);
			if (i==0)
				paint.setTextAlign(Align.LEFT);
			paint.setColor(Color.WHITE);
			canvas.drawText(horlabels[i], x, height - 4, paint);
		}

		paint.setTextAlign(Align.CENTER);
		canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

		paint.setColor(Color.LTGRAY);
		if (type == BAR) {
			float datalength = values.size();
			float colwidth = (width - (2 * border)) / datalength;
			for (int i = 0; i < values.size(); i++) {
				float val = Math.max(values.get(i) - MIN, 0);
				float rat = val / diff;
				float h = graphheight * rat;
				canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
			}
		} else {
			float datalength = values.size();
			float colwidth = (width - (2 * border)) / datalength;
			float halfcol = colwidth / 2;
			float lasth = 0;
			for (int i = 0; i < values.size(); i++) {
				float val = Math.max(values.get(i) - MIN, 0);
				float rat = val / diff;
				float h = graphheight * rat;
				if (i > 0)
					canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
				lasth = h;
			}
		}
	}
}
