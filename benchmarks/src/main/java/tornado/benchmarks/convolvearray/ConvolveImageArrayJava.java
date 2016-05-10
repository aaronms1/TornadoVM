package tornado.benchmarks.convolvearray;

import tornado.benchmarks.BenchmarkDriver;
import static tornado.benchmarks.BenchmarkUtils.createFilter;
import static tornado.benchmarks.BenchmarkUtils.createImage;
import static tornado.benchmarks.GraphicsKernels.*;
public class ConvolveImageArrayJava extends BenchmarkDriver {

	private final int imageSizeX, imageSizeY, filterSize;
	
	private float[] input, output, filter;
	
	public ConvolveImageArrayJava(int iterations, int imageSizeX, int imageSizeY, int filterSize){
		super(iterations);
		this.imageSizeX = imageSizeX;
		this.imageSizeY = imageSizeY;
		this.filterSize = filterSize;
	}
	
	@Override
	public void setUp() {
		input = new float[imageSizeX * imageSizeY];
        output = new float[imageSizeX * imageSizeY];
        filter = new float[filterSize * filterSize];

        createImage(input, imageSizeX, imageSizeY);
        createFilter(filter, filterSize, filterSize);

	}
	
	@Override
	public void tearDown() {
		input = null;
		output = null;
		filter = null;
		super.tearDown();
	}

	@Override
	public void code() {
			convolveImageArray(input,filter,output,imageSizeX,imageSizeY,filterSize,filterSize);
	}
	
	@Override
	public void barrier(){
		
	}

	@Override
	public boolean validate() {
		return true;
	}
	
	public void printSummary(){
		System.out.printf("id=java-serial, elapsed=%f, per iteration=%f\n",getElapsed(),getElapsedPerIteration());
	}

}