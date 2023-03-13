/*
 * Douglas Crocker Simple Stock Manager
 */
 
 
 
import java.util.ArrayList;
import java.util.Iterator;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.scene.Group;
import javafx.scene.chart.Axis; 
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.layout.*;
import javafx.geometry.*;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;




/**
 * A custom candlestick chart.
 *
 * @see javafx.scene.chart.Axis
 * @see javafx.scene.chart.Chart
 * @see javafx.scene.chart.NumberAxis
 * @see javafx.scene.chart.XYChart
 */


class CandleStickChartScene extends Pane {
	

    public CandleStickChartScene() {
			
			
        // x-axis:
        final NumberAxis xAxis = new NumberAxis(0, 32, 1);
        xAxis.setMinorTickCount(0);
        xAxis.setLabel("Day");

        // y-axis:
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price");
        yAxis .setForceZeroInRange(false);
		
		//ticker		
		String ticker = "TWTR.txt";

        // chart:
        final CandleStickChart bc = new CandleStickChart(xAxis, yAxis);
        bc.setTitle("TWTR");
		
        // add starting data (changed for loop)
        XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
		
		try
		{
			for (int l = 0; l < 31; l++) {
				double[][] data = Stock.returnInfo(ticker);
				series.getData().add(
						new XYChart.Data<Number, Number>(data[l][0], data[l][1], new CandleStickExtraValues(data[l][2], data[l][3], data[l][4], data[l][5]))
				);
			}
		} catch (FileNotFoundException e){
			System.out.println("file not found");
		}
		
        ObservableList<XYChart.Series<Number, Number>> data = bc.getData();
        if (data == null) {
            data = FXCollections.observableArrayList(series);
            bc.setData(data);
        } else {
            bc.getData().add(series);
        }
		

        getChildren().add(bc);
		
		
    }

    /**
     * A candlestick chart is a style of bar-chart used primarily to describe
     * price movements of a security, derivative, or currency over time.
     *
     * The Data Y value is used for the opening price and then the close, high
     * and low values are stored in the Data's extra value property using a
     * CandleStickExtraValues object.
     */
    private class CandleStickChart extends XYChart<Number, Number> {

        // -------------- CONSTRUCTORS ----------------------------------------------
        /**
         * Construct a new CandleStickChart with the given axis.
         *
         * @param xAxis The x axis to use
         * @param yAxis The y axis to use
         */
        public CandleStickChart(Axis<Number> xAxis, Axis<Number> yAxis) {
            super(xAxis, yAxis);
            setAnimated(false);
            xAxis.setAnimated(false);
            yAxis.setAnimated(false);
        }

        /**
         * Construct a new CandleStickChart with the given axis and data.
         *
         * @param xAxis The x axis to use
         * @param yAxis The y axis to use
         * @param data The data to use, this is the actual list used so any
         * changes to it will be reflected in the chart
         */
        public CandleStickChart(Axis<Number> xAxis, Axis<Number> yAxis, ObservableList<Series<Number, Number>> data) {
            this(xAxis, yAxis);
            setData(data);
        }

        // -------------- METHODS ------------------------------------------------------------------------------------------
        /**
         * Called to update and layout the content for the plot
         */
        @Override
        protected void layoutPlotChildren() {
            // we have nothing to layout if no data is present
            if (getData() == null) {
                return;
            }
            // update candle positions
            for (int seriesIndex = 0; seriesIndex < getData().size(); seriesIndex++) {
                Series<Number, Number> series = getData().get(seriesIndex);
                Iterator<Data<Number, Number>> iter = getDisplayedDataIterator(series);
                Path seriesPath = null;
                if (series.getNode() instanceof Path) {
                    seriesPath = (Path) series.getNode();
                    seriesPath.getElements().clear();
                }
                while (iter.hasNext()) {
                    Data<Number, Number> item = iter.next();
                    double x = getXAxis().getDisplayPosition(getCurrentDisplayedXValue(item));
                    double y = getYAxis().getDisplayPosition(getCurrentDisplayedYValue(item));
                    Node itemNode = item.getNode();
                    CandleStickExtraValues extra = (CandleStickExtraValues) item.getExtraValue();
                    if (itemNode instanceof Candle && extra != null) {
                        Candle candle = (Candle) itemNode;

                        double close = getYAxis().getDisplayPosition(extra.getClose());
                        double high = getYAxis().getDisplayPosition(extra.getHigh());
                        double low = getYAxis().getDisplayPosition(extra.getLow());
                        // calculate candle width
                        double candleWidth = -1;
                        if (getXAxis() instanceof NumberAxis) {
                            NumberAxis xa = (NumberAxis) getXAxis();
                            candleWidth = xa.getDisplayPosition(xa.getTickUnit()) * 0.90; // use 90% width between ticks
                        }
                        // update candle
                        candle.update(close - y, high - y, low - y, candleWidth);
                        candle.updateTooltip(item.getYValue().doubleValue(), extra.getClose(), extra.getHigh(), extra.getLow());

                        // position the candle
                        candle.setLayoutX(x);
                        candle.setLayoutY(y);
                    }
                    if (seriesPath != null) {
                        if (seriesPath.getElements().isEmpty()) {
                            seriesPath.getElements().add(new MoveTo(x, getYAxis().getDisplayPosition(extra.getAverage())));
                        } else {
                            seriesPath.getElements().add(new LineTo(x, getYAxis().getDisplayPosition(extra.getAverage())));
                        }
                    }
                }
            }
        }

        @Override
        protected void dataItemChanged(Data<Number, Number> item) {
        }

        @Override
        protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
            Node candle = createCandle(getData().indexOf(series), item, itemIndex);
            if (shouldAnimate()) {
                candle.setOpacity(0);
                getPlotChildren().add(candle);
                // fade in new candle
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(1);
                ft.play();
            } else {
                getPlotChildren().add(candle);
            }
            // always draw average line on top
            if (series.getNode() != null) {
                series.getNode().toFront();
            }
        }

        @Override
        protected void dataItemRemoved(Data<Number, Number> item, Series<Number, Number> series) {
            final Node candle = item.getNode();
            if (shouldAnimate()) {
                // fade out old candle
                FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                ft.setToValue(0);
                ft.setOnFinished(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent actionEvent) {
                        getPlotChildren().remove(candle);
                    }
                });
                ft.play();
            } else {
                getPlotChildren().remove(candle);
            }
        }

        @Override
        protected void seriesAdded(Series<Number, Number> series, int seriesIndex) {
            // handle any data already in series
            for (int j = 0; j < series.getData().size(); j++) {
                Data item = series.getData().get(j);
                Node candle = createCandle(seriesIndex, item, j);
                if (shouldAnimate()) {
                    candle.setOpacity(0);
                    getPlotChildren().add(candle);
                    // fade in new candle
                    FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                    ft.setToValue(1);
                    ft.play();
                } else {
                    getPlotChildren().add(candle);
                }
            }
            // create series path
            Path seriesPath = new Path();
            seriesPath.getStyleClass().setAll("candlestick-average-line", "series" + seriesIndex);
            series.setNode(seriesPath);
            getPlotChildren().add(seriesPath);
        }

        @Override
        protected void seriesRemoved(Series<Number, Number> series) {
            // remove all candle nodes
            for (XYChart.Data<Number, Number> d : series.getData()) {
                final Node candle = d.getNode();
                if (shouldAnimate()) {
                    // fade out old candle
                    FadeTransition ft = new FadeTransition(Duration.millis(500), candle);
                    ft.setToValue(0);
                    ft.setOnFinished(new EventHandler<ActionEvent>() {

                        @Override
                        public void handle(ActionEvent actionEvent) {
                            getPlotChildren().remove(candle);
                        }
                    });
                    ft.play();
                } else {
                    getPlotChildren().remove(candle);
                }
            }
        }

        /**
         * Create a new Candle node to represent a single data item
         *
         * @param seriesIndex The index of the series the data item is in
         * @param item The data item to create node for
         * @param itemIndex The index of the data item in the series
         * @return New candle node to represent the give data item
         */
        private Node createCandle(int seriesIndex, final Data item, int itemIndex) {
            Node candle = item.getNode();
            // check if candle has already been created
            if (candle instanceof Candle) {
                ((Candle) candle).setSeriesAndDataStyleClasses("series" + seriesIndex, "data" + itemIndex);
            } else {
                candle = new Candle("series" + seriesIndex, "data" + itemIndex);
                item.setNode(candle);
            }
            return candle;
        }

        /**
         * This is called when the range has been invalidated and we need to
         * update it. If the axis are auto ranging then we compile a list of all
         * data that the given axis has to plot and call invalidateRange() on
         * the axis passing it that data.
         */
        @Override
        protected void updateAxisRange() {
            // For candle stick chart we need to override this method as we need to let the axis know that they need to be able
            // to cover the whole area occupied by the high to low range not just its center data value
            final Axis<Number> xa = getXAxis();
            final Axis<Number> ya = getYAxis();
            List<Number> xData = null;
            List<Number> yData = null;
            if (xa.isAutoRanging()) {
                xData = new ArrayList<Number>();
            }
            if (ya.isAutoRanging()) {
                yData = new ArrayList<Number>();
            }
            if (xData != null || yData != null) {
                for (Series<Number, Number> series : getData()) {
                    for (Data<Number, Number> data : series.getData()) {
                        if (xData != null) {
                            xData.add(data.getXValue());
                        }
                        if (yData != null) {
                            CandleStickExtraValues extras = (CandleStickExtraValues) data.getExtraValue();
                            if (extras != null) {
                                yData.add(extras.getHigh());
                                yData.add(extras.getLow());
                            } else {
                                yData.add(data.getYValue());
                            }
                        }
                    }
                }
                if (xData != null) {
                    xa.invalidateRange(xData);
                }
                if (yData != null) {
                    ya.invalidateRange(yData);
                }
            }
        }
    }

    /**
     * Data extra values for storing close, high and low.
     */
    private class CandleStickExtraValues {

        private double close;
        private double high;
        private double low;
        private double average;

        public CandleStickExtraValues(double close, double high, double low, double average) {
            this.close = close;
            this.high = high;
            this.low = low;
            this.average = average;
        }

        public double getClose() {
            return close;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public double getAverage() {
            return average;
        }
    }

    /**
     * Candle node used for drawing a candle
     */
    private class Candle extends Group {

        private Line highLowLine = new Line();
        private Region bar = new Region();
        private String seriesStyleClass;
        private String dataStyleClass;
        private boolean openAboveClose = true;
        private Tooltip tooltip = new Tooltip();

        private Candle(String seriesStyleClass, String dataStyleClass) {
            setAutoSizeChildren(false);
            getChildren().addAll(highLowLine, bar);
            this.seriesStyleClass = seriesStyleClass;
            this.dataStyleClass = dataStyleClass;
            updateStyleClasses();
            tooltip.setGraphic(new TooltipContent());
            Tooltip.install(bar, tooltip);
        }

        public void setSeriesAndDataStyleClasses(String seriesStyleClass, String dataStyleClass) {
            this.seriesStyleClass = seriesStyleClass;
            this.dataStyleClass = dataStyleClass;
            updateStyleClasses();
        }

        public void update(double closeOffset, double highOffset, double lowOffset, double candleWidth) {
            openAboveClose = closeOffset > 0;
            updateStyleClasses();
            highLowLine.setStartY(highOffset);
            highLowLine.setEndY(lowOffset);
            if (candleWidth == -1) {
                candleWidth = bar.prefWidth(-1);
            }
            if (openAboveClose) {
                bar.resizeRelocate(-candleWidth / 2, 0, candleWidth, closeOffset);
            } else {
                bar.resizeRelocate(-candleWidth / 2, closeOffset, candleWidth, closeOffset * -1);
            }
        }

        public void updateTooltip(double open, double close, double high, double low) {
            TooltipContent tooltipContent = (TooltipContent) tooltip.getGraphic();
            tooltipContent.update(open, close, high, low);
//                    tooltip.setText("Open: "+open+"\nClose: "+close+"\nHigh: "+high+"\nLow: "+low);
        }

        private void updateStyleClasses() {
            getStyleClass().setAll("candlestick-candle", seriesStyleClass, dataStyleClass);
            highLowLine.getStyleClass().setAll("candlestick-line", seriesStyleClass, dataStyleClass,
                    openAboveClose ? "open-above-close" : "close-above-open");
            bar.getStyleClass().setAll("candlestick-bar", seriesStyleClass, dataStyleClass,
                    openAboveClose ? "open-above-close" : "close-above-open");
        }
    }

    private class TooltipContent extends GridPane {

        private Label openValue = new Label();
        private Label closeValue = new Label();
        private Label highValue = new Label();
        private Label lowValue = new Label();

        private TooltipContent() {
            Label open = new Label("OPEN:");
            Label close = new Label("CLOSE:");
            Label high = new Label("HIGH:");
            Label low = new Label("LOW:");
            open.getStyleClass().add("candlestick-tooltip-label");
            close.getStyleClass().add("candlestick-tooltip-label");
            high.getStyleClass().add("candlestick-tooltip-label");
            low.getStyleClass().add("candlestick-tooltip-label");
            setConstraints(open, 0, 0);
            setConstraints(openValue, 1, 0);
            setConstraints(close, 0, 1);
            setConstraints(closeValue, 1, 1);
            setConstraints(high, 0, 2);
            setConstraints(highValue, 1, 2);
            setConstraints(low, 0, 3);
            setConstraints(lowValue, 1, 3);
            getChildren().addAll(open, openValue, close, closeValue, high, highValue, low, lowValue);
        }

        public void update(double open, double close, double high, double low) {
            openValue.setText(Double.toString(open));
            closeValue.setText(Double.toString(close));
            highValue.setText(Double.toString(high));
            lowValue.setText(Double.toString(low));
        }
    }
	
	
}



public class DouglasCrockerIA extends Application {
		
    @Override
    public void start(Stage stage) {

		/*
		*
	    	* Contains implementation for entering custom stock ticket symbols
		*
		
		TextField Tfticker = new TextField("Enter ticker Here");
		
		String ticker;
		
		EventHandler<ActionEvent> event = new EventHandler<ActionEvent>(){
			public void handle(ActionEvent a)
			{
				ticker = Tfticker.getText();
				ticker = ticker + ".txt";
				
			}
		};
		*/
		 
		/*
		* tools for storing and calculating balance of stock
		*/
		class BalanceTools
		{
			public int numofshares = 1;
			public double currentbalance;
			
			public double getBuyBalance(double curbalance, double price){
				double returnbalance;
				double total = price * numofshares; 
				curbalance = curbalance - total; 
				if(curbalance >= 0) {
					returnbalance = curbalance;
				} else {
					System.out.println("Not Enough Money!");
					returnbalance = currentbalance; 
				}
				return returnbalance; 
			}
			public double getSellBalance(double curbalance, double price){
			double x = price * numofshares; 
			curbalance = curbalance + x;

				return curbalance;
			}
			
			
			public int getNumShares(String text){
				numofshares = Integer.valueOf(text);
				return numofshares;
			}
		}
		
		
		
		
		class TFtools
		{
			public double beforeBalance;
			public double price;
			public double[][] stockArr;
			public double tfday;
			
			public double getDay(String text){	
				tfday = Double.valueOf(text);
				return tfday;
			}
			
			public double getPrice(double day, double[][] stock){
						
				int j = 0;
				price = 0;
				
				while(price == 0){
					if(day == stock[j][0]){
						price = stock[j][1];
					
					} else {
						j++;
					}
				}
			return price;
						
			}

			public double[][] stockInfo(){
						
				try
				{
					stockArr = Stock.returnInfo("TWTR.txt");
				}catch (FileNotFoundException g){
					System.out.println("file not found");
				}
			return stockArr;
			}
			
		}
		
		
	//class containing contents of an owned Stock Posistion	
	class IndivPosition {
		
		int day = 0;
		double price = 0;
		double numofshares = 0;
		double balanceBefore = 0;
		double balanceAfter = 0; 
		
		IndivPosition(int day, double price, double numofshares, double balanceBefore, double balanceAfter) {
		
		
			this.day = day;
			this.price = price;
			this.numofshares = numofshares; 
			this.balanceBefore = balanceBefore;
			this.balanceAfter = balanceAfter;
		
		}
		
		// For converting Individual Position to string for display within GUI
		@Override
		public String toString() {
			
			String day = String.valueOf(this.day);
			String price = String.valueOf(this.price);
			String numofshares = String.valueOf(this.numofshares);
			String balanceBefore = String.valueOf(this.balanceBefore);
			String balanceAfter = String.valueOf(this.balanceAfter);
			
			return (day + ("     ") + price + ("             ") + numofshares + ("                   ") + balanceBefore + ("                ") + balanceAfter);
		
		}
	
	}
	
	
	//Main class containing all of users stock posistions 
	class portfolio implements Serializable{
		IndivPosition UserPositions[];
		int index = 0;
		int totalpositions = 0;
		
		portfolio(){
			UserPositions = new IndivPosition[100];
		}
	}
	
	
		//Main Portfolio declaration 
		portfolio MainPortfolio = new portfolio();
		
		//Set Initial Balance 
		BalanceTools BalTools = new BalanceTools();
		
		BalTools.currentbalance = 1000;
		
		
		//Label for Current Balance
		Label lbcurrentbal = new Label("Balance is " + BalTools.currentbalance);
		
		
		//BorderPane
		BorderPane border = new BorderPane();
		Scene borderscene = new Scene(border);
		stage.setScene(borderscene);
		
		borderscene.getStylesheets().add("CandleChart_StyleSheet.css");
		
		//HBox
		HBox hbox = new HBox();
		hbox.setPadding(new Insets(15, 12, 15, 12));
		hbox.setSpacing(10);
		
		Label Exceptionresponse = new Label();
		
		//ObservableList for listview
		ObservableList<IndivPosition> Stocks = FXCollections.observableArrayList();
		
		/*
		
		Buy Button Logic and GUI
		
		*/

		Button buy = new Button("Buy");
		buy.setStyle("-fx-background-color: #00ff00");
		buy.setPrefSize(100, 20);
			
			
			buy.setOnAction(e-> {
				
				Exceptionresponse.setText("");
				
				Button cancel = new Button("Cancel");
				cancel.setStyle("-fx-background-color: gray");
				
				BorderPane buyborder = new BorderPane();
				
				VBox vb = new VBox();
				
				HBox hb = new HBox();
				hb.setPadding(new Insets(15, 12, 15, 12));
				hb.setSpacing(10);
				
				HBox hbbalance = new HBox();
				hbbalance.setPadding(new Insets(50, 20, 50, 150));
		        hbbalance.setSpacing(10);
				
				Button btnbuyconf = new Button("Confirm Buy");
				
				TextField tfbuyday = new TextField("Day");
				TextField tfbuynum = new TextField("Number of Shares");
				
				
				
					btnbuyconf.setOnAction((ActionEvent p) -> {
						
						TFtools buytools = new TFtools();
						
						buytools.getDay(tfbuyday.getText());
						
						BalTools.getNumShares(tfbuynum.getText());
						
							
						if (BalTools.getBuyBalance(BalTools.currentbalance, buytools.getPrice(buytools.tfday, buytools.stockInfo())) >= 0) {
							
						double Shareamount = BalTools.numofshares;
						
						double balanceBefore = BalTools.currentbalance;
						
						double price = buytools.getPrice(buytools.tfday, buytools.stockInfo());
						
						BalTools.currentbalance = BalTools.getBuyBalance(BalTools.currentbalance, buytools.getPrice(buytools.tfday, buytools.stockInfo()));
						
						double balanceAfter = BalTools.currentbalance;
						
						double checker = balanceBefore - price;
						
						if(checker >= 0){
						
						Stocks.add(new IndivPosition((int)buytools.tfday, price, Shareamount, balanceBefore, balanceAfter));
						} else {
							System.out.println("Not enough money");
						}
						MainPortfolio.totalpositions = MainPortfolio.totalpositions + (int)Shareamount; 
						
						} else {
							Exceptionresponse.setText("Insufficient Funds");
						}
						
						Exceptionresponse.setText("");						
						lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
							
							
					});
				
				
				vb.getChildren().add(cancel);
				
				hbbalance.getChildren().add(lbcurrentbal);
				
				hb.getChildren().addAll(tfbuyday, tfbuynum, btnbuyconf);
				
				buyborder.setLeft(vb);
				buyborder.setTop(hbbalance);
				buyborder.setCenter(hb);
				buyborder.setBottom(Exceptionresponse);
				
				Scene b = new Scene (buyborder, 500, 300);
				stage.setScene(b);
				cancel.setOnAction(f->{
					
				stage.setScene(borderscene);
				
				lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
				
				});
				
			
			});
			
			lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
	
		/*
		
		Sell Button logic and GUI
		
		*/
		
		Button sell = new Button("Sell");
		sell.setPrefSize(100, 20);
		sell.setStyle("-fx-background-color: #ff0000");
		
			
			sell.setOnAction(e-> {
				
				Exceptionresponse.setText("");
				
				Button cancel = new Button("Cancel");
				cancel.setStyle("-fx-background-color: gray");
				
				BorderPane sellborder = new BorderPane();
				
				VBox vb = new VBox();
				
				HBox hb = new HBox();
				hb.setPadding(new Insets(15, 12, 15, 12));
				hb.setSpacing(10);
				
				HBox hbbalance = new HBox();
				hbbalance.setPadding(new Insets(50, 20, 50, 150));
		        hbbalance.setSpacing(10);
				
				Button sellconf = new Button("Confirm Sell");
				
				TextField tfsellday = new TextField("Day");
				TextField tfsellnum = new TextField("Number of Shares");
				
				
				
				
					sellconf.setOnAction((ActionEvent p) -> {
						
						TFtools selltools = new TFtools();
						
						selltools.getDay(tfsellday.getText());
						
						BalTools.getNumShares(tfsellnum.getText());
						
						if(BalTools.numofshares < MainPortfolio.totalpositions){
							
							try {
								
							double Shareamount = BalTools.numofshares;
						
							double balanceBefore = BalTools.currentbalance;
						
							double price = selltools.getPrice(selltools.tfday, selltools.stockInfo());
									
							BalTools.currentbalance = BalTools.getSellBalance(BalTools.currentbalance, selltools.getPrice(selltools.tfday, selltools.stockInfo()));
							
							double balanceAfter = BalTools.currentbalance;
							
							Stocks.add(new IndivPosition((int)selltools.tfday, price, -Shareamount, balanceBefore, balanceAfter));
							
							MainPortfolio.totalpositions = MainPortfolio.totalpositions - (int)Shareamount;
							
							}
							catch(ArrayIndexOutOfBoundsException exception) {
								
							System.out.println("Day does not exist");
							
							}
							
						
						} else {
							System.out.println("Shares not owned");
						}
						
						Exceptionresponse.setText("");
						lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
						
						
							
							
					});
					
					
				Exceptionresponse.setText("");
				
				vb.getChildren().add(cancel);
				
				hbbalance.getChildren().add(lbcurrentbal);
				
				hb.getChildren().addAll(tfsellday, tfsellnum, sellconf);
				
				sellborder.setLeft(vb);
				sellborder.setTop(hbbalance);
				sellborder.setCenter(hb);
				sellborder.setBottom(Exceptionresponse);
				
				Scene s = new Scene (sellborder, 500, 300);
				stage.setScene(s);
				cancel.setOnAction(f->{
				
				stage.setScene(borderscene);
				
				lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
				});
				
			
			});
			
			lbcurrentbal.setText("Current balance is: " + BalTools.currentbalance);
			

			/*
				
			ListView
			
			*/
			
			
			ListView<IndivPosition> lvPortfolio = new ListView<IndivPosition>(Stocks);
			lvPortfolio.setEditable(true);
			lvPortfolio.setPrefSize(300, 200);
			
			// Label Header for ListView
			Label lbheader = new Label("\n                                          Positions \n \n Day  Price   Number of Shares   Balance before   balance after");
			
				

				
		VBox PosBox = new VBox();	

		PosBox.getChildren().addAll(lbheader, lvPortfolio);
		
		hbox.getChildren().addAll(buy, sell);
		
		border.setLeft(PosBox);
		border.setRight(hbox);
		border.setCenter(new CandleStickChartScene());
		border.setTop(lbcurrentbal);
		
		stage.setTitle("TradeView");
		
	
        stage.setScene(borderscene);
        stage.show();
		
	
    }

    public static void main(String[] args) {
        launch(args);
    }
	
}
