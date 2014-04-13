package com.barobot.gui.utils;

import com.barobot.R;

public class PhotoGallery {
	private final int galerySize = 16;
	private int[] ids;
	private int defaultImageID;
	
	public PhotoGallery()
	{
		Setup();
	}
	
	private void Setup()
	{
		defaultImageID = R.drawable.image_drink_03;
		ids = new int [galerySize+1];
		ids[0] = R.drawable.image_drink_00;
		ids[1] = R.drawable.image_drink_01;
		ids[2] = R.drawable.image_drink_02;
		ids[3] = R.drawable.image_drink_03;
		ids[4] = R.drawable.image_drink_04;
		ids[5] = R.drawable.image_drink_05;
		ids[6] = R.drawable.image_drink_06;
		ids[7] = R.drawable.image_drink_07;
		ids[8] = R.drawable.image_drink_08;
		ids[9] = R.drawable.image_drink_09;
		ids[10] = R.drawable.image_drink_10;
		ids[11] = R.drawable.image_drink_11;
		ids[12] = R.drawable.image_drink_12;
		ids[13] = R.drawable.image_drink_13;
		ids[14] = R.drawable.image_drink_14;
		ids[15] = R.drawable.image_drink_15;	
	}
	
	public int getImageID(int photoId)
	{
		if (photoId < 1 || photoId > galerySize)
		{
			return defaultImageID;
		}
		return ids[photoId];
	}
}