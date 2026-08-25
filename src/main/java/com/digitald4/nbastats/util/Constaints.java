package com.digitald4.nbastats.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Constaints {
	public static final DateTimeFormatter COMPUTER_DATE = DateTimeFormat.forPattern("yyyy-MM-dd");

	public enum FantasyLeague {
		DRAFT_KINGS("draftkings", 50000),
		FAN_DUEL("fanduel", 60000);

		public String name;
		public int salaryCap;

		FantasyLeague(String name, int salaryCap) {
			this.name = name;
			this.salaryCap = salaryCap;
		}
	}

	public static int getSeason(DateTime date) {
		return date.getYear() + (date.getMonthOfYear() > 9 ?  1 : 0);
	}

	public static int getPrevSeason(DateTime date) {
		return getSeason(date) - 1;
	}
}
