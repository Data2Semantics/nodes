package org.nodes.util;

import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * Credits: http://bbgen.net/blog/2011/06/java-svg-to-bufferedimage/
 * 
 * @author Peter
 * 
 */
public class BufferedImageTranscoder extends ImageTranscoder
{

	@Override
	public BufferedImage createImage(int w, int h)
	{
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		return bi;
	}

	@Override
	public void writeImage(BufferedImage img, TranscoderOutput output)
	{
		this.img = img;
	}

	public BufferedImage getBufferedImage()
	{
		return img;
	}

	private BufferedImage img = null;
}