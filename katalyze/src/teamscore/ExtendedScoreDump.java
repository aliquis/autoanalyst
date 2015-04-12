package teamscore;

import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import web.StaticWebDocument;
import web.WebPublisher;

import model.Contest;
import model.FakeScore;
import model.OutputHook;
import model.Problem;
import model.Score;
import model.ScoreTableComparer;
import model.ScoreTableEntry;
import model.Standings;
import model.StandingsPublisher;
import model.Team;

public class ExtendedScoreDump implements OutputHook, StandingsPublisher {
	static final Logger log = Logger.getLogger(ExtendedScoreDump.class);

	final Contest contest;
	final WebPublisher publisherTarget;
	final static ScoreTableComparer comparator = new ScoreTableComparer();

	class ScoreDumper {
		Standings standings;
		int minutesFromStart;
		ArrayList<Score> scoresAbove = new ArrayList<Score>();


		public ScoreDumper(Standings standings, int minutesFromStart) {
			this.standings = standings;
			this.minutesFromStart = minutesFromStart;
		}

		public JSONObject DumpScore(Score score) {

			Team team = score.getTeam();

			JSONArray problems = new JSONArray();

			int place = scoresAbove.size();
			for (Problem p : contest.getProblems()) {
				boolean isSolved = score.isSolved(p);
				JSONObject problemInfo = new JSONObject()
					.element("id", p.getLetter())
					.element("solved", isSolved)
					.element("attempts", score.submissionCount(p))
					.element("time", score.scoreContribution(p));

                int lastSubmissionTime = score.lastSubmissionTime(p);
                if (lastSubmissionTime != 0) {
                    problemInfo = problemInfo.element("lastUpd", lastSubmissionTime);
                }

				if (!isSolved) {
					ScoreTableEntry fake = FakeScore.PretendProblemSolved(score, p, minutesFromStart);
					JSONObject potential = new JSONObject();
					place = calcFictiousRank(scoresAbove, fake, place, potential);
					problemInfo = problemInfo.element("potential", potential);
				}
				String language = team.languageFor(p);
				if (language != null) {
					problemInfo = problemInfo.element("lang", language);
				}
				problems.add(problemInfo);
			}


			JSONObject target = new JSONObject()
				.element("rank", standings.rankOf(team))
				.element("team", new JSONObject()
					.element("id", team.getTeamNumber())
					.element("tag", team.toString())
					.element("name", team.getName()))
				.element("nSolved", score.getNumberOfSolvedProblems())
				.element("totalTime", score.getTimeIncludingPenalty())
				.element("mainLang", team.getMainLanguage())
				.element("problems", problems);

			return target;
		}

		private JSONArray getProblems(Contest contest) {
			JSONArray result = new JSONArray();

			for (Problem p : contest.getProblems()) {
				JSONObject problemInfo = new JSONObject()
					.element("tag", p.getLetter())
					.element("name", p.getName());
				result.add(problemInfo);
			}

			return result;
		}

		private JSONObject getContestInfo(Contest contest) {
			return new JSONObject()
                    .element("length", contest.getLengthInMinutes())
				.element("problems", getProblems(contest))
				.element("submissions", contest.getSubmissionCount())
                .element("time", contest.getMinutesFromStart());
		}

		public String execute() {
			scoresAbove.clear();

			JSONArray resultArray = new JSONArray();
			ArrayList<JSONObject> jsonScores = new ArrayList<JSONObject>();

			for (Score score : standings) {
				scoresAbove.add(score);
				jsonScores.add(DumpScore(score));
			}

			resultArray.addAll(jsonScores);

			JSONObject contestInfo = getContestInfo(standings.getContest());

			JSONObject contestStatus = new JSONObject()
				.element("scoreBoard", resultArray)
				.element("contestInfo", contestInfo);

			return contestStatus.toString();
		}

		private int calcFictiousRank(ArrayList<Score> scoresAbove,
									 ScoreTableEntry fake, int startFrom, JSONObject result) {

			int fakeIndex = startFrom;

			while (fakeIndex > 0 && comparator.compare(fake, scoresAbove.get(fakeIndex - 1)) <= 0) {
				fakeIndex--;
			}
			while (fakeIndex < scoresAbove.size() && comparator.compare(fake, scoresAbove.get(fakeIndex)) > 0) {
				fakeIndex++;
			}
			int margin = -1;
			result.element("rank", fakeIndex + 1);
			if (fakeIndex < scoresAbove.size()) {
				ScoreTableEntry next = scoresAbove.get(fakeIndex);
				if (next.getNumberOfSolvedProblems() == fake.getNumberOfSolvedProblems()) {
					margin = next.getTimeIncludingPenalty() - fake.getTimeIncludingPenalty();
					result.element("before", margin);
				}
			}
			return fakeIndex;
		}


	}

	public ExtendedScoreDump(Contest contest, WebPublisher target) {
		this.contest = contest;
		this.publisherTarget = target;
	}

	public void publishStandings() {
		int minutesFromStart = contest.getMinutesFromStart();
		int submissionsAtTime = contest.getSubmissionsAtTime(minutesFromStart);

		ScoreDumper scoreDumper = new ScoreDumper(contest.getStandings(submissionsAtTime), minutesFromStart);
		String scoreTable = scoreDumper.execute();

		StaticWebDocument scoreDoc = new StaticWebDocument("application/json", scoreTable);
		log.debug("publishing Standings... " + minutesFromStart);
		publisherTarget.publish("/Standings", scoreDoc);
	}


	@Override
	public void execute(int minutesFromStart) {
		log.debug("preparing Standings... " + minutesFromStart);
		int submissionsAtTime = contest.getSubmissionsAtTime(minutesFromStart);

		ScoreDumper scoreDumper = new ScoreDumper(contest.getStandings(submissionsAtTime), minutesFromStart);
		log.debug("dumping Standings... " + minutesFromStart);
		String scoreTable = scoreDumper.execute();

		StaticWebDocument scoreDoc = new StaticWebDocument("application/json", scoreTable);
		log.debug("publishing Standings... " + minutesFromStart);
		publisherTarget.publish("/Standings", scoreDoc);
		publisherTarget.publish(String.format("/Standings.%03d", minutesFromStart), scoreDoc);
		log.debug("done publishing Standings... " + minutesFromStart);

	}



}
