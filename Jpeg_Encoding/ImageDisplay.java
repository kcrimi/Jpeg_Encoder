
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {
	
  
   public static void main(String[] args) {
   	

	String fileName = args[0];
	int quantLevel = Integer.parseInt(args[1]);
	int deliveryMode = Integer.parseInt(args[2]);
	int latency = Integer.parseInt(args[3]);
	
   	int width =  352;
	int height = 288;
	
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    try {
	    File file = new File(fileName);
	    InputStream is = new FileInputStream(file);

	    long len = file.length();
	    byte[] bytes = new byte[(int)len];
	    byte[] outBytes = new byte[(int) len];
	    
	    int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
        
    	//create input image	
    	int ind = 0;
		for(int y = 0; y < height; y++){
	
			for(int x = 0; x < width; x++){
		 
				byte a = 0;
				byte r = bytes[ind];
				byte g = bytes[ind+height*width];
				byte b = bytes[ind+height*width*2]; 
				
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				img.setRGB(x,y,pix);
				ind++;
			}
		}
		
		//encoder 
		double[] block = new double[64];
		int blockInd;
		long[] outCoefs = new long[(int) len];
		double[] DCTBlock = new double[64];
		
		for (int rgbInd = 0; rgbInd < 3; rgbInd++)
		{
			for (int blockY = 0; blockY < height; blockY+=8)
			{
				for (int blockX = 0; blockX < width; blockX+=8)
				{
					//populate block
					blockInd = 0;
					while (blockInd<64)
					{
						
						block[blockInd] = (bytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] & 0xFF);
						blockInd++;
					}
					
					//perform DCT
					double C, sumTerm;
					for (int v = 0; v < 8; v++)
					{
						for (int u = 0; u < 8; u++)
						{
							C = .25;
							if (u==0 && v==0)
							{
								C *= .5;
							}else if (u==0 || v==0)
							{
								C *= (1/Math.sqrt(2.0));
							}
							
							sumTerm = 0;
							for (int y = 0; y < 8; y++)
							{
								for (int x = 0; x < 8; x++)
								{
									sumTerm += (block[x + (y * 8)] * Math.cos(((2.0*x +1 )*u*Math.PI)/16.0) * Math.cos(((2.0*y +1 )*v*Math.PI)/16.0));
								}
							}
							DCTBlock[u + (v*8)] = (C * sumTerm);
						}
					}
					
					//copy block to outBytes array
					blockInd = 0;
					while (blockInd<64)
					{
						outCoefs[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] =  Math.round(DCTBlock[blockInd]/(Math.pow(2, quantLevel)));
						blockInd++;
					}
				}
			}
		}
		
		// Use a label to display the image
	    JFrame frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    JPanel contentFrame = new JPanel();
	    JLabel label = new JLabel(new ImageIcon(img));
	    JLabel label2 = new JLabel(new ImageIcon(outImg));
	    contentFrame.add(label);
	    contentFrame.add(label2);
	    frame.add(contentFrame);
	    
	    frame.pack();
	    frame.setVisible(true);
	    
	    
		//Decoders
		switch (deliveryMode)
		{
		
		//Instant Delivery
		case 0: 
			for (int rgbInd = 0; rgbInd < 3; rgbInd++)
			{
				for (int blockY = 0; blockY < height; blockY+=8)
				{
					for (int blockX = 0; blockX < width; blockX+=8)
					{
						//populate block
						blockInd = 0;
						while (blockInd<64)
						{
							DCTBlock[blockInd] = outCoefs[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] * Math.pow(2, quantLevel);
							blockInd++;
						}
						
						//perform Inverse DCT
						double C, sumTerm;
						for (int y = 0; y < 8; y++)
						{
							for (int x = 0; x < 8; x++)
							{
								sumTerm = 0;
								for (int v = 0; v < 8; v++)
								{
									for (int u = 0; u < 8; u++)
									{
										C = 1.0;
										if (u==0 && v==0)
										{
											C *= 0.5;
										}else if (u==0 || v==0)
										{
											C *= (1.0/Math.sqrt(2.0));
										}
										
										sumTerm += C * DCTBlock[u + (v*8)] * Math.cos(((2.0*x + 1.0 )*u*Math.PI)/16.0) * Math.cos(((2.0*y +1.0 )*v*Math.PI)/16.0);
									}
								}
								block[x + (y*8)] = (.25 * sumTerm);
							}
						}
						
						//copy block to outBytes array
						blockInd = 0;
						while (blockInd<64)
						{
							if (block[blockInd]>255)
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 255;
							}else if (block[blockInd]<0)
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 0;
							}else
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) block[blockInd];
							}
							blockInd++;
						}
						
					}
				}
			}
			ind = 0;
			for(int y = 0; y < height; y++){
				
				for(int x = 0; x < width; x++){
			 
					byte a = 0;
					byte r = outBytes[ind];
					byte g = outBytes[ind+height*width];
					byte b = outBytes[ind+height*width*2]; 
					
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					outImg.setRGB(x,y,pix);
					ind++;
				}
			}
			label2.setIcon(new ImageIcon(outImg));
			break;
		
		//Sequential Delivery
		case 1: 
		for (int blockY = 0; blockY < height; blockY+=8)
			{
				for (int blockX = 0; blockX < width; blockX+=8)
				{
					for (int rgbInd = 0; rgbInd < 3; rgbInd++)
					{
						//populate block
						blockInd = 0;
						while (blockInd<64)
						{
							DCTBlock[blockInd] = outCoefs[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] * Math.pow(2, quantLevel);
							blockInd++;
						}
						
						//perform Inverse DCT
						double C, sumTerm;
						for (int y = 0; y < 8; y++)
						{
							for (int x = 0; x < 8; x++)
							{
								sumTerm = 0;
								for (int v = 0; v < 8; v++)
								{
									for (int u = 0; u < 8; u++)
									{
										C = 1.0;
										if (u==0 && v==0)
										{
											C *= 0.5;
										}else if (u==0 || v==0)
										{
											C *= (1.0/Math.sqrt(2.0));
										}
										
										sumTerm += C * DCTBlock[u + (v*8)] * Math.cos(((2.0*x + 1.0 )*u*Math.PI)/16.0) * Math.cos(((2.0*y +1.0 )*v*Math.PI)/16.0);
									}
								}
								block[x + (y*8)] = (.25 * sumTerm);
							}
						}
						
						//copy block to outBytes array
						blockInd = 0;
						while (blockInd<64)
						{
							if (block[blockInd]>255)
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 255;
							}else if (block[blockInd]<0)
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 0;
							}else
							{
								outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) block[blockInd];
							}
							blockInd++;
						}
						
					}
					for(int y = 0; y < 8; y++){
						
						for(int x = 0; x < 8; x++){
							
							ind = (blockX + x) + ((blockY + y) * width);
							byte a = 0;
							byte r = outBytes[ind];
							byte g = outBytes[ind+height*width];
							byte b = outBytes[ind+height*width*2]; 
							
							int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
							//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
							outImg.setRGB((blockX+x),(blockY+y),pix);
							ind++;
						}
					}
					label2.setIcon(new ImageIcon(outImg));
					Thread.sleep(latency);
				}
			}
			break;
			
		//Spectral Selection	
		case 2: 
			int[] zigZag = {0,1,8,16,9,2,3,10,17,24,32,25,18,11,4,5,12,19,26,33,40,48,41,39,27,20,13,6,7,19,21,28,35,42,49,56,57,50,43,36,29,22,15,23,30,37,44,51,58,59,52,45,38,31,39,46,53,60,61,54,47,55,62,63};
			int u1,v1;
			for (int zStop=0; zStop < zigZag.length; zStop++)
			{
				for (int rgbInd = 0; rgbInd < 3; rgbInd++)
				{
					for (int blockY = 0; blockY < height; blockY+=8)
					{
						for (int blockX = 0; blockX < width; blockX+=8)
						{
							//populate block
							blockInd = 0;
							while (blockInd<64)
							{
								DCTBlock[blockInd] = outCoefs[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] * Math.pow(2, quantLevel);
								blockInd++;
							}
						
							//perform Inverse DCT
							double C, sumTerm;
							for (int y = 0; y < 8; y++)
							{
								for (int x = 0; x < 8; x++)
								{
									sumTerm = 0;
									for (int zInd = 0; zInd <= zStop; zInd++)
									{
											u1 = (zInd % 8); 			
											v1 = (zInd / 8);
											C = 1.0;
											if (u1==0 && v1==0)
											{
												C *= 0.5;
											}else if (u1==0 || v1==0)
											{
												C *= (1.0/Math.sqrt(2.0));
											}
											
											sumTerm += C * DCTBlock[u1 + (v1*8)] * Math.cos(((2.0*x + 1.0 )*u1*Math.PI)/16.0) * Math.cos(((2.0*y +1.0 )*v1*Math.PI)/16.0);
									}
									block[x + (y*8)] = (.25 * sumTerm);
								}
							}
						
							//copy block to outBytes array
							blockInd = 0;
							while (blockInd<64)
							{
								if (block[blockInd]>255)
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 255;
								}else if (block[blockInd]<0)
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 0;
								}else
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) block[blockInd];
								}
								blockInd++;
							}
						}						
					}
				}
				ind = 0;
				for(int y = 0; y < height; y++){
					
					for(int x = 0; x < width; x++){
				 
						byte a = 0;
						byte r = outBytes[ind];
						byte g = outBytes[ind+height*width];
						byte b = outBytes[ind+height*width*2]; 
						
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						outImg.setRGB(x,y,pix);
						ind++;
					}
				}
				label2.setIcon(new ImageIcon(outImg));
				Thread.sleep(latency);
			}
			break;
			
		//Successive Bit Approximation	
		case 3: 
			for (int cutOff = 1; cutOff <=16; cutOff++)
			{
				for (int rgbInd = 0; rgbInd < 3; rgbInd++)
				{
					for (int blockY = 0; blockY < height; blockY+=8)
					{
						for (int blockX = 0; blockX < width; blockX+=8)
						{
							//populate block
							blockInd = 0;
							while (blockInd<64)
							{
								DCTBlock[blockInd] = bitFilter(outCoefs[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] * Math.pow(2, quantLevel), cutOff);
								blockInd++;
							}
							
							//perform Inverse DCT
							double C, sumTerm;
							for (int y = 0; y < 8; y++)
							{
								for (int x = 0; x < 8; x++)
								{
									sumTerm = 0;
									for (int v = 0; v < 8; v++)
									{
										for (int u = 0; u < 8; u++)
										{
											C = 1.0;
											if (u==0 && v==0)
											{
												C *= 0.5;
											}else if (u==0 || v==0)
											{
												C *= (1.0/Math.sqrt(2.0));
											}
											
											sumTerm += C * DCTBlock[u + (v*8)] * Math.cos(((2.0*x + 1.0 )*u*Math.PI)/16.0) * Math.cos(((2.0*y +1.0 )*v*Math.PI)/16.0);
										}
									}
									block[x + (y*8)] = (.25 * sumTerm);
								}
							}
							
							//copy block to outBytes array
							blockInd = 0;
							while (blockInd<64)
							{
								if (block[blockInd]>255)
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 255;
								}else if (block[blockInd]<0)
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) 0;
								}else
								{
									outBytes[(height*width*rgbInd) + (blockX + (blockInd % 8)) + ((blockY + (blockInd / 8)) * width)] = (byte) block[blockInd];
								}
								blockInd++;
							}
							
						}
					}
				}
				ind = 0;
				for(int y = 0; y < height; y++)
				{
					for(int x = 0; x < width; x++)
					{

						byte a = 0;
						byte r = outBytes[ind];
						byte g = outBytes[ind+height*width];
						byte b = outBytes[ind+height*width*2]; 

						
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
						outImg.setRGB(x,y,pix);
						ind++;
					}
				}
				label2.setIcon(new ImageIcon(outImg));
				Thread.sleep(latency);
			}
		}
			
		
		
		
		
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
		e.printStackTrace();
	}

   }
 
   static long bitFilter (double inputDouble, int cutOff)
   // Used to pare down bytes to significant bits in successive bit approximation decoding
   {
	   short input = (short) Math.abs(inputDouble);
	   
		int msbCount = 0;
		short sigByte =  0;
		for (int bitShift = 16; bitShift>=0; bitShift--)
		{
			if (msbCount == cutOff)
			{
				break;
			}
			if ((input & (1 << bitShift)) != 0)
			{
				sigByte = (short) (sigByte | (0x1<<bitShift));
				
				msbCount++;
			}
		}
			if (inputDouble<0)
		{ 
			return -sigByte;
		}
		return sigByte;
		
   }
}