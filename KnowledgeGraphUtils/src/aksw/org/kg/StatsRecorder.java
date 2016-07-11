package aksw.org.kg;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class can be used to keep track of
 * general statistical information.
 * 
 * This class is thread-safe 
 * 
 * @author kay
 *
 */
public class StatsRecorder {
	
	/** map which keeps all the statistics */
	protected Map<String, AtomicLong> statistics = new LinkedHashMap<>();
	
	protected Map<String, Set<String>> specialOutput = new LinkedHashMap<>();
	
	/** make sure that we change the data correctly */
	static Lock lock = new ReentrantLock(true);
	
	/**
	 * This method can be used to get a stats entry value
	 * 
	 * @param statId
	 * @return
	 */
	public long getStatsEntry(final Class<?> classEntry, final String statId) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		return statsValue.get();
	}
	
	/**
	 * This method sets a stat entry
	 * 
	 * @param statId
	 * @param value
	 */
	public void setStats(final Class<?> classEntry, final String statId, final long value) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		statsValue.lazySet(value);		
	}
	
	public void setStatText(final Class<?> classEntry, final String statId, final String text) {
		
		String id = new StringBuffer().append(classEntry.getName()).append("/").append(statId).toString();
		Set<String> texts = this.specialOutput.get(id);
		if (null == texts) {
			texts = new LinkedHashSet<>();
			this.specialOutput.put(id, texts);
		}
		
		texts.add(text);
	}
	
	/**
	 * This method can be used to increment a stats entry by 1
	 * 
	 * @param statId
	 */
	public long incrementStats(final Class<?> classEntry, final String statId) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		return statsValue.incrementAndGet();
	}
	
	/**
	 * This method can be used to decrement a stats entry by 1
	 * 
	 * @param statId
	 */
	public void decrementStats(final Class<?> classEntry, final String statId) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		statsValue.decrementAndGet();
	}
	
	/**
	 * This method can be used to add an amount to a stats entry
	 * 
	 * @param statId	- stat id
	 * @param add		- number which should be added
	 */
	public void addNumberToStats(final Class<?> classEntry, final String statId, final long add) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		statsValue.addAndGet(add);
	}
	
	/**
	 * This method can be used to substract an amount to a stats entry
	 * @param statId
	 * @param sub
	 */
	public void subNumberToStats(final Class<?> classEntry, final String statId, final long sub) {
		AtomicLong statsValue = this.checkForStatsValue(classEntry, statId);
		statsValue.addAndGet((-1L) * sub);
	}
	
	/**
	 * This method can be used to check whether
	 * an entry for an stats id exists.
	 * 
	 * If not, it is added to the statistics block
	 * 
	 * @param statId	- statistics id
	 * @return Atomic Long instance
	 */
	protected AtomicLong checkForStatsValue(final Class<?> classEntry, final String statId) {
		
		String id = new StringBuffer().append(classEntry.getName()).append("/").append(statId).toString();
		AtomicLong value = this.statistics.get(id);
		
		if (null == value) {
			lock.lock();
			try {
				// only make that change, if nobodoy else has done it yet
				if (false == this.statistics.containsKey(id)) {
					value = new AtomicLong(0);
					this.statistics.put(id, value);
				} else {
					// make sure we get to correct atomic number
					value = this.statistics.get(id);
				}
			} finally {
				lock.unlock();
			}
		}
		
		return value;
	}
	
	public String getStatistics() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Number stats: \n");
		for (String statEntry : this.statistics.keySet()) {
			builder.append("< ");
			builder.append(statEntry);
			builder.append(" = ");
			builder.append(this.statistics.get(statEntry));
			builder.append(" >\n");
		}
		
		builder.append("\nSpecial Output: \n");
		for (String statEntry : this.specialOutput.keySet()) {
			builder.append("< ");
			builder.append(statEntry);
			builder.append("(");
			builder.append(this.specialOutput.size());
			builder.append(")");
			builder.append(" = ");
			builder.append(this.specialOutput.get(statEntry));
			builder.append(" >\n");
		}
		
		return builder.toString();
	}
	
//	class Address {
//		long foundAddress = 0;
//		long foundNoAddress = 0;
//	}	
//	Address address = new Address();
//	
//	long entityCount = 0;
//	long tripleCount = 0;
//	long emptyTrippleCount = 0;
//	
//	long totalTime = 0;
//	long maxTimeForEntity = 0;
//	long minTimeForEntity = Long.MAX_VALUE;
}
