package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import rules.StandingsCriterion;
import rules.Criteria.*;


public class StandingsCriteria extends ContestSimulatorTest {
	

	private void assertFulfilled(boolean expected, StandingsCriterion criterion) {
		assertEquals(expected, criterion.isFulfilled(contest.getStandings()));
	}
	
	@Test
	public void problemSolvedByAll() {
		InitContest(5, 10);
		StandingsCriterion oneProblemSolvedByAll = new AllTeamsSolvedOneProblem();
		
		for (int i=0; i<10; i++) {
			assertFulfilled(false, oneProblemSolvedByAll);
			Accepted(teams[i], problems[0], i);
		}
		assertFulfilled(true, oneProblemSolvedByAll);
	}

	@Test
	public void allProblemsSolved() {
		InitContest(5, 10);
		StandingsCriterion allProblemsSolved = new AllProblemsSolved();
		
		for (int i=0; i<5; i++) {
			assertFulfilled(false, allProblemsSolved);
			Accepted(teams[i], problems[i], i);
		}
		assertFulfilled(true, allProblemsSolved);
	}	

}
