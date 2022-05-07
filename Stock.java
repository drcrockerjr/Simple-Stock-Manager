import java.io.*;
import java.util.*;
import java.lang.*;
import java.lang.Double;


	
public class Stock{

	static class candle {
		double day;
		double open;
		double close;
		double high;
		double low;
		double average;
		
		 candle(double day, double open, double close, double high, double low, double average) {
			 this.day = day;
			 this.open = open;
			 this.close = close;
			 this.high = high;
			 this.low = low;
			 this.average = average;
		 }
	}
	
	
    public static double[][] returnInfo(String filename) throws FileNotFoundException
	{ 
		
		File inFile = new File(filename);
		int l = 0;
		int size = 0;
		String readline;
		double readnum;
		List<candle> list = new ArrayList<candle>();
		double[] candleval = new double[6];
			try 
			{
				Scanner scan = new Scanner(inFile);

				while (scan.hasNextLine()) {
					readline = scan.nextLine();
					String[] stringcandle = readline.split(" ");
						for(int i = 0; i < 5; i++){
							
							candleval[i] = Double.valueOf(stringcandle[i]);
						}
					list.add(new candle(candleval[0], candleval[1], candleval[2], candleval[3], candleval[4], candleval[5]));
				}//while
				scan.close();
			}
			catch(FileNotFoundException ex)
			{
			System.out.println("please enter a file that exists");
			}

		double[][] arr = new double[list.size()][6];
		
		for (int i = 0; i < list.size(); i++)
		{
		candle temp = list.get(i);
			arr[i][0] =  temp.day; 
			arr[i][1] = temp.open;
			arr[i][2] = temp.close;
			arr[i][3] = temp.high;
			arr[i][4] = temp.low;
			arr[i][5] = temp.average;
		

		}
		
		return (arr);
	
	}//returnInfo
	public static void main(String[] args) throws FileNotFoundException
	{
		double[][] a;
		
		a = returnInfo("TWTR.txt");
		
		
		for(int i = 0; i < 25; i++){
			System.out.println(a[i][0] + " " + a[i][1] + " " + a[i][2] + " " + a[i][3] + " " + a[i][4] + " " + a[i][5]);
		}
	}
	}//Stock
