package io.bittiger.ads;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;

import org.json.*;

public class AdsEngine {

    private String mAdsDataFilePath;
    private String mBudgetFilePath;
    private IndexBuilder indexBuilder;
    private String mMemcachedServer;
    private int mMemcachedPortal;
    private String mysql_host;
    private String mysql_db;
    private String mysql_user;
    private String mysql_pass;

    public AdsEngine(String adsDataFilePath, String budgetDataFilePath,String memcachedServer,int memcachedPortal,String mysqlHost,String mysqlDb,String user,String pass)
    {
        mAdsDataFilePath = adsDataFilePath;
        mBudgetFilePath = budgetDataFilePath;
        mMemcachedServer = memcachedServer;
        mMemcachedPortal = memcachedPortal;
        mysql_host = mysqlHost;
        mysql_db = mysqlDb;
        mysql_user = user;
        mysql_pass = pass;
        indexBuilder = new IndexBuilder(memcachedServer,memcachedPortal,mysql_host,mysql_db,mysql_user,mysql_pass);
    }

    public Boolean init()
    {
        //load ads data
        try (BufferedReader brAd = new BufferedReader(new FileReader(mAdsDataFilePath))) {
            String line;
            while ((line = brAd.readLine()) != null) {
                JSONObject adJson = new JSONObject(line);
                Ad ad = new Ad();
                if(adJson.isNull("adId") || adJson.isNull("campaignId")) {
                    continue;
                }
                ad.adId = adJson.getLong("adId");
                ad.campaignId = adJson.getLong("campaignId");
                ad.brand = adJson.isNull("brand") ? "" : adJson.getString("brand");
                ad.price = adJson.isNull("price") ? 100.0 : adJson.getDouble("price");
                ad.thumbnail = adJson.isNull("thumbnail") ? "" : adJson.getString("thumbnail");
                ad.title = adJson.isNull("title") ? "" : adJson.getString("title");
                ad.detail_url = adJson.isNull("detail_url") ? "" : adJson.getString("detail_url");
                ad.bidPrice = adJson.isNull("bidPrice") ? 1.0 : adJson.getDouble("bidPrice");
                ad.pClick = adJson.isNull("pClick") ? 0.0 : adJson.getDouble("pClick");
                ad.category =  adJson.isNull("category") ? "" : adJson.getString("category");
                ad.description = adJson.isNull("description") ? "" : adJson.getString("description");
                ad.keyWords = new ArrayList<String>();
                JSONArray keyWords = adJson.isNull("keyWords") ? null :  adJson.getJSONArray("keyWords");
                for(int j = 0; j < keyWords.length();j++)
                {
                    ad.keyWords.add(keyWords.getString(j));
                }
                indexBuilder.buildInvertIndex(ad);

				if(!indexBuilder.buildInvertIndex(ad) || !indexBuilder.buildForwardIndex(ad))
				{
					//log
				}

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        //load budget data
        try (BufferedReader brBudget = new BufferedReader(new FileReader(mBudgetFilePath))) {
            String line;
            while ((line = brBudget.readLine()) != null) {
                JSONObject campaignJson = new JSONObject(line);
                Long campaignId = campaignJson.getLong("campaignId");
                double budget = campaignJson.getDouble("budget");
                Campaign camp = new Campaign();
                camp.campaignId = campaignId;
                camp.budget = budget;
				if(!indexBuilder.updateBudget(camp))
				{
					//log
				}
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        indexBuilder.Close();
        return true;
    }
}
