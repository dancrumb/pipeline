package com.cobalt.bamboo.plugin.pipeline.Controllers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.cobalt.bamboo.plugin.pipeline.cache.WallBoardCache;
import com.cobalt.bamboo.plugin.pipeline.cache.WallBoardData;
import com.cobalt.bamboo.plugin.pipeline.cdperformance.UptimeGrade;
import com.cobalt.bamboo.plugin.pipeline.cdresult.CDResult;

public class CacheManagerImpl implements CacheManager {
	private final MainManager mainManager;
	private TransactionTemplate transactionTemplate;
	private WallBoardCache wallBoardCache;
	
	private AtomicBoolean firstLoadDone;
	
	/**
	 * Constructs a CacheManager object.
	 * 
	 * @param mainManager The MainManager to get CDResults from.
	 * @param transactionTemplate The TransactionTemplate to allow access to the Bamboo
	 *                            database from outside of a web request.
	 */
	public CacheManagerImpl(MainManager mainManager, TransactionTemplate transactionTemplate){
		this.mainManager = mainManager;
		this.transactionTemplate = transactionTemplate;
		wallBoardCache = new WallBoardCache();
		
		firstLoadDone = new AtomicBoolean(false);
	}

	@Override
	public void putAllWallBoardData() {
		transactionTemplate.execute(new TransactionCallback() {
			
			@Override
			public Object doInTransaction() {
				
				refreshCache();
				
				firstLoadDone.compareAndSet(false, true);
				
				return null;
			}
		});
	}
	
	@Override
	public void updateWallBoardDataForPlan(final String planKey, final boolean updateUptimeGrade) {
		transactionTemplate.execute(new TransactionCallback() {
			
			@Override
			public Object doInTransaction() {
				
				CDResult cdResult = mainManager.getCDResultForPlan(planKey);
				
				UptimeGrade uptimeGrade;
				if (!wallBoardCache.containsPlan(planKey)) {
					uptimeGrade = mainManager.getUptimeGradeForPlan(planKey);
				} else {
					uptimeGrade = wallBoardCache.get(planKey).uptimeGrade;
					if (updateUptimeGrade) {
						uptimeGrade.update(cdResult.getCurrentBuild());
					}
				}
				
				
				if (cdResult != null && uptimeGrade != null) {
					WallBoardData wallBoardData = new WallBoardData(planKey, cdResult, uptimeGrade);
					wallBoardCache.put(planKey, wallBoardData);
				}
				
				return null;
			}
		});
	}

	@Override
	public List<WallBoardData> getAllWallBoardData() {
		if (firstLoadDone.get() == false) {
			return null;
		}
		
		List<WallBoardData> results = wallBoardCache.getAllWallBoardData();
		
		return results;
	}

	@Override
	public void clearCache() {
		wallBoardCache.clear();
	}
	
	/*
	 * A private method that does a full refresh on the cache.
	 * This method is factored out so that it can be called inside or
	 * outside of the TransactionTemplate depending on the context.
	 */
	private void refreshCache() {
		WallBoardCache newCache = new WallBoardCache();
		List<CDResult> cdResults = mainManager.getCDResults();
		
		for (CDResult cdResult : cdResults) {
			String planKey = cdResult.getPlanKey();
			UptimeGrade uptimeGrade = mainManager.getUptimeGradeForPlan(planKey);
			
			WallBoardData wallBoardData = new WallBoardData(planKey, cdResult, uptimeGrade);
			
			newCache.put(planKey, wallBoardData);
		}
		
		wallBoardCache = newCache;
	}
}
