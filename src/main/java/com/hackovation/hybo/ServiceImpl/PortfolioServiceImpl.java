package com.hackovation.hybo.ServiceImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityTransaction;

import org.algo.finance.FinanceUtils;
import org.algo.finance.data.GoogleSymbol;
import org.algo.finance.data.GoogleSymbol.Data;
import org.algo.finance.portfolio.BlackLittermanModel;
import org.algo.finance.portfolio.MarketEquilibrium;
import org.algo.matrix.BasicMatrix;
import org.algo.matrix.BigMatrix;
import org.algo.series.CalendarDateSeries;
import org.algo.type.CalendarDate;
import org.algo.type.CalendarDateUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hack17.hybo.domain.Allocation;
import com.hack17.hybo.domain.Fund;
import com.hack17.hybo.domain.IndexPrice;
import com.hack17.hybo.domain.InvestorProfile;
import com.hack17.hybo.domain.Portfolio;
import com.hack17.hybo.domain.RiskTolerance;
import com.hack17.hybo.repository.PortfolioRepository;
import com.hackovation.hybo.AllocationType;
import com.hackovation.hybo.ReadFile;
import com.hackovation.hybo.Util.EtfIndexMap;
import com.hackovation.hybo.bean.ProfileRequest;
import com.hackovation.hybo.services.PortfolioService;

@Service
@Transactional
public class PortfolioServiceImpl implements PortfolioService{

	@Autowired
	PortfolioRepository portfolioRepository;

	Map<String,String> indexToEtfMap;
	Map<String,String> EtfToIndexMap;

	
	@Override
	public Map<String,Portfolio> buildPortfolio(InvestorProfile profile,int clientId,boolean dummy,Date date,int investment) {
		System.out.println("Building Portfolio Started "+clientId);
		EtfToIndexMap = EtfIndexMap.getEtfToIndexMapping();
		indexToEtfMap = EtfIndexMap.getIndexToEtfMapping();
		// Step 1. Calculate Covariance Matrix
		BasicMatrix covarianceMatrix = getCovarianceMatrix(date);
		System.out.println("--------------Covariance Matrix Calculation "+clientId);
//		System.out.println(covarianceMatrix);

		// Step 2. Calculate Lambda (Risk Aversion Factor)
		Double lambda = 2.5d;
		
		
		String[] tickers = getAssetsTickers();
//		double[][] marketWeight = {{0.25},{3},{0.25},{0.25}};
		System.out.println("--------------Fetching Market Weight Calculation"+clientId);
		double[][] marketWeight = getMarketWeight();
		BasicMatrix marketWeightMatrix =  BigMatrix.FACTORY.rows(marketWeight);
		MarketEquilibrium marketEquilibrium = new MarketEquilibrium(tickers, covarianceMatrix, lambda);
		BlackLittermanModel bl  = new BlackLittermanModel(marketEquilibrium, marketWeightMatrix);
		List<BigDecimal> weights = new ArrayList<>();
		weights.add(new BigDecimal(0));
		weights.add(new BigDecimal(0));
		weights.add(new BigDecimal(0));
		weights.add(new BigDecimal(0));
		weights.add(new BigDecimal(0));
		weights.add(new BigDecimal(0));
		bl.addViewWithBalancedConfidence(weights, 0.26);
		
/*		System.out.println("--------------Asset Return Matrix---------------------");
		System.out.println(bl.getAssetReturns());
*/	
		System.out.println("--------------Asset Weights Calculation Started "+clientId);
		BasicMatrix finalAssetWeights = bl.getAssetWeights();
		System.out.println("--------------Asset Weights "+finalAssetWeights);
		LinkedHashMap<String, Double> assetClassWiseWeight = new LinkedHashMap<>();
		long i = 0;
		for(String assetClass:indexToEtfMap.keySet()){
			assetClassWiseWeight.put(assetClass, finalAssetWeights.doubleValue(i++));
		}
		Map<String,Portfolio> map = buildPortfolio(profile,investment,assetClassWiseWeight,clientId,dummy);
		System.out.println("Building Portfolio Done "+clientId);
		return map;		
	}

	private LinkedHashMap<String,String> getassetETFMap(){
		LinkedHashMap<String,String> assetETFMap = new LinkedHashMap<>();
		assetETFMap.put("CRSPTM1","VTI");
		assetETFMap.put("CRSPLC1","VTV");
		assetETFMap.put("CRSPMI1","VOE");
		assetETFMap.put("CRSPSC1","VBR");
		assetETFMap.put("SHV","SHV");
		assetETFMap.put("LQD","LQD");
		return assetETFMap;
	}
	private String[] getAssetsTickers(){
		LinkedHashMap<String,String> assetETFMap = getassetETFMap();
		String[] arr = new String[assetETFMap.keySet().size()];
		assetETFMap.keySet().toArray(arr);
		return arr;
	}
	private String[] getAssetsTickersOld(){
		String[] tickers = new String[5];
		tickers[0] = "AOR";
		tickers[1] = "MDIV";
		tickers[2] = "AOM";
		tickers[3] = "PCEF";
		tickers[4] = "AOA";
		tickers[5] = "SHV";
		tickers[4] = "LQD";
		return tickers;
	}
	double[][] getMarketWeight(){
		double[][] marketWeight = new double[6][1];
		double[] totalValue = new double[6]; 
		try{
			ArrayList<String> files = new ArrayList<>(6);
			files.add("CRSP_US_Total_Market_IndividualMarketValue.txt");
			files.add("CRSP_US_Large_Cap_Value_IndividualMarketValue.txt");
			files.add("CRSP_US_MID_CAP_VALUE_IndividualMarketValue.txt");
			files.add("CRSP_US_SMALL_CAP_VALUE_IndividualMarketValue.txt");
			files.add("SHV_IndividualMarketValue.txt");
			files.add("LQD_IndividualMarketValue.txt");
			int i = 0;
			ClassLoader cl = getClass().getClassLoader();
			for(String fileName:files){
				File file = new File(cl.getResource(fileName).getFile());
				BufferedReader f = new BufferedReader(new FileReader(file));
				String ln=null;
				while((ln=f.readLine())!=null){
					totalValue[i] +=Double.valueOf(ln.substring(ln.lastIndexOf("#")+1));
				}
				i++;
				f.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		double total=0;
		for(double d:totalValue)
			total+=d;
		marketWeight[0][0]=totalValue[0]/total;
		marketWeight[1][0]=totalValue[1]/total;
		marketWeight[2][0]=totalValue[2]/total;
		marketWeight[3][0]=totalValue[3]/total;
		marketWeight[4][0]=totalValue[4]/total;
		marketWeight[5][0]=totalValue[5]/total;
		return marketWeight;
	}
	BasicMatrix getCovarianceMatrix(Date date){
		Collection<CalendarDateSeries<Double>> col = new ArrayList<>();
		col.add(getCalendarDataSeriesFromDatabase("CRSPTM1",date));
		col.add(getCalendarDataSeriesFromDatabase("CRSPLC1",date));
		col.add(getCalendarDataSeriesFromDatabase("CRSPMI1",date));
		col.add(getCalendarDataSeriesFromDatabase("CRSPSC1",date));
		col.add(getCalendarDataSeriesFromDatabase("SHV",date));
		col.add(getCalendarDataSeriesFromDatabase("LQD",date));
		BasicMatrix covarianceMatrix = FinanceUtils.makeCovarianceMatrix(col);
		return covarianceMatrix;
	}
	public CalendarDateSeries<Double> getCalendarDataSeriesFromDatabase(String symbol,Date portfolioDate){
		CalendarDateSeries<Double> series = new CalendarDateSeries<Double>(CalendarDateUnit.DAY).name(symbol);
		try{
			List<IndexPrice> indexPriceList = portfolioRepository.getIndexPrice(symbol,portfolioDate);
			for(IndexPrice indexPrice:indexPriceList){
				CalendarDate key = new CalendarDate(indexPrice.getDate());
				
				NumberFormat numberFormat = NumberFormat.getInstance(java.util.Locale.US);
				series.put(key, indexPrice.getPrice());
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return series;
	}

	public Map<String,Portfolio> buildPortfolio(InvestorProfile profile,int investment,LinkedHashMap<String, Double> assetClassWiseWeight,int clientId,boolean dummy){
		Portfolio portfolio = new Portfolio();
		portfolio.setClientId(clientId);
		List<Allocation> allocationList = new ArrayList<>();
		Map<String, Portfolio> portfolioMap = new HashMap<>();
		Date date = new Date();
		for(String assetClass:indexToEtfMap.keySet()){
			Allocation allocation = new Allocation();
			Fund fund = new Fund();
	 		GoogleSymbol gs = new GoogleSymbol(indexToEtfMap.get(assetClass));
	 		List<Data> dataList = gs.getHistoricalPrices();
	 		Double cost = investment*assetClassWiseWeight.get(assetClass);
	 		if(dataList != null && dataList.size()>0){
	 			Data data = dataList.get(0);
	 			double perIndexCost = data.getPrice();
	 			NumberFormat nf = NumberFormat.getInstance();
	 			System.out.println("Asset Class: "+assetClass+" Weight: "+assetClassWiseWeight.get(assetClass)+" Cost: "+cost +" PerIndexCost: "+perIndexCost);
	 			allocation.setQuantity(Double.valueOf((cost/perIndexCost)).intValue());
	 			allocation.setCostPrice(allocation.getQuantity()*perIndexCost);
	 			allocation.setInvestment(investment);
	 			allocation.setPercentage(assetClassWiseWeight.get(assetClass)*100);
	 			allocation.setType(AllocationType.EQ.name());
	 			allocation.setTransactionDate(date);
	 			allocation.setIsActive("Y");
	 			fund.setTicker(indexToEtfMap.get(assetClass));
	 			allocation.setFund(fund);
	 			allocationList.add(allocation);
	 		}
		}
		portfolio.setAllocations(allocationList);
		//System.out.println(portfolioRepository.getPortfolio(1));
		
		portfolio.setInvestorProfile(profile);
		portfolioRepository.persist(portfolio);
		
		portfolioMap.put(clientId+"", portfolio);
		return portfolioMap;
	}

	@Override
	public void deleteAllPortfolio() {
		List<Portfolio> listOfPortfolios =  portfolioRepository.getAllPortfolios();
		
		for(Portfolio port:listOfPortfolios)portfolioRepository.delete(port);
	}

	@Override
	public InvestorProfile createProfile(ProfileRequest profileRequest) {
		InvestorProfile profile = new InvestorProfile();
		profile.setAnnualIncome(profileRequest.getIncome());
		profile.setRiskTolerance(RiskTolerance.valueOf(profileRequest.getRisk().toUpperCase()));
		int horizonOffsetYear = profileRequest.getTime();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, horizonOffsetYear);
		profile.setHorizonAsOfDate(cal.getTime());
		portfolioRepository.persist(profile);
		return profile;
	}
}
