import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Collections;


public class runner {
	static int DATASET_SIZE;
	static int ITERATIONS;
	static int RANK_STARTDO = 300;
	static int RANK_ENDDO = 300;
	static int RANK_SECOND_TO_LAST = 300;
	static int RANK_CHORD_TONE_STRONG=100;
	static int RANK_CHORD_TONE_GEN=30;
	static int RANK_DIATONIC_EVER=2400;
	static int RANK_STEPWISE = 100;
	static int RANK_CHORDAL_LEAP=20;
	static int RANK_LEAP_PUNISH_R1 = 150;
	static int RANK_LEAP_PUNISH_R2 = 250;
	static int RANK_LEAP_PUNISH_R3 = 250;
	static int RANK_LEAP_PUNISH_R4 = 1000;
	static int RANK_LEAP_PUNISH_TRI = 500;
	static int RANK_CHANGE_ON_STRONG = 200;
	static int RANK_CHANGE_FROM_PREV_STRONG = 200;
	static int RANK_QUARTER_NOTE_REWARD = 200;
	static int RANK_DOUBLE_EIGHT_REWARD = 200;
	static int RANK_RANGE_REWARD = 500;


	static int firstNote = 0;
	static int lastNote = 0;
	static int lowestNote = 0;
	static int highestNote = 0;

	static Boolean[] SCALE = new Boolean[12];
	static Boolean[][] CHORDS = new Boolean[4][12];


	static int MELODY_LENGTH = 4*4*4+1; 	//4 bars, 4/4 16th as the smallest divison, plus a down beat
	

	static int[][] chordTones = {{0,4,7},{5,9,0},{2,5,9},{2,7,11,5},{0,4}};
	static String log = "[0";
	static String TIMESTAMP = "";

	//melody values are just midi
	//-1 = rest
	//init an cool dataset of RANDOM :D (size? 100 or something to start)
	static ArrayList<Melody> dataset = new ArrayList<>();

	public static void main(String[] args) {
		DATASET_SIZE = Integer.parseInt(args[0]);
		ITERATIONS = Integer.parseInt(args[1]);
		TIMESTAMP = args[2];

		RANK_STARTDO = Integer.parseInt(args[3]);
		RANK_ENDDO = Integer.parseInt(args[4]);
		RANK_SECOND_TO_LAST = Integer.parseInt(args[5]);
		RANK_CHORD_TONE_STRONG=Integer.parseInt(args[6]);
		RANK_CHORD_TONE_GEN=Integer.parseInt(args[7]);
		RANK_DIATONIC_EVER=Integer.parseInt(args[8]);
		RANK_STEPWISE = Integer.parseInt(args[9]);
		RANK_CHORDAL_LEAP=Integer.parseInt(args[10]);
		RANK_LEAP_PUNISH_R1 = Integer.parseInt(args[11]);
		RANK_LEAP_PUNISH_R2 = Integer.parseInt(args[12]);
		RANK_LEAP_PUNISH_R3 = Integer.parseInt(args[13]);
		RANK_LEAP_PUNISH_R4 = Integer.parseInt(args[14]);
		RANK_LEAP_PUNISH_TRI = Integer.parseInt(args[15]);
		RANK_CHANGE_ON_STRONG = Integer.parseInt(args[16]);
		RANK_CHANGE_FROM_PREV_STRONG = Integer.parseInt(args[17]);
		RANK_QUARTER_NOTE_REWARD = Integer.parseInt(args[18]);
		RANK_DOUBLE_EIGHT_REWARD = Integer.parseInt(args[19]);
		RANK_RANGE_REWARD = Integer.parseInt(args[20]);

		firstNote = Integer.parseInt(args[21]);
		lastNote = Integer.parseInt(args[22]);
		lowestNote = Integer.parseInt(args[23]);
		highestNote = Integer.parseInt(args[24]);

		int s = 25;
		for(int z = 0; z <12; z ++){
			SCALE[z] = Boolean.parseBoolean(args[z+s]);
		}
		s = s+12;
		for(int z = 0; z<4; z++){
			for(int x = 0; x < 12; x++){
				CHORDS[z][x] = Boolean.parseBoolean(args[s+z*12+x]);
			}
		}
		//Ok, now we have pulled everything from the void or args[]

		Comparator<Melody> comparator = new MelodyComp();

		for(int i = 0; i < DATASET_SIZE; i ++){
			dataset.add(new Melody(MELODY_LENGTH));
			dataset.get(i).fitness = rank(dataset.get(i));
		}
		//k. That inits our dataset now.
		
		System.out.println("Initial Dataset");
		
		//now we do everything.
		for(int i = 0; i < ITERATIONS; i ++){
       		PriorityQueue<Melody> queue = new PriorityQueue<Melody>(DATASET_SIZE, comparator);
			//rank every melody, put it in a queue
			for(int j = 0; j <DATASET_SIZE; j ++){
				queue.add(dataset.get(j));
			}
			
			//clear the old dataset, about to make a new one
			dataset.clear();

			//"breed" the best melodies
			if(Math.random()>.5){queue.poll();}
			//50% chance of discarding the most fit
			//prevents case where the top tier are paird mating with eachother not changing

			for(int j = 0; j <DATASET_SIZE/4; j ++){
				Melody curA = queue.poll();
				Melody curB = queue.poll();
				Melody childA = Melody.breed(curA, curB);
				Melody childB = Melody.breed(curA, curB);
				Melody childC = Melody.breed(curA, curB);
				dataset.add(childA);
				dataset.add(childB);
				dataset.add(childC);
			}

			//Add back in the less-good melodies
			while(dataset.size() < DATASET_SIZE){
				dataset.add(queue.poll());
			}
			//do something about mutating them
			for(int j = 0; j <DATASET_SIZE; j ++){
				dataset.set(j,Melody.mutate(dataset.get(j)));
				dataset.get(j).fitness = rank(dataset.get(j));
			}
			vitals(i);
		}

		for(int i = 0; i <DATASET_SIZE; i++){
			dataset.get(i).fitness = rank(dataset.get(i));
		}
		try{
			Collections.sort(dataset,comparator);	
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(dataset);
			return;
		}
		System.out.println("Finished, final Dataset");
		printDataset("out/"+TIMESTAMP);
	}

	public static void printDataset(String location){
		try {
			PrintWriter writer = new PrintWriter(location+"/melody.ly", "UTF-8");
			writer.println("#(set-global-staff-size 25)");
			writer.println("\\paper {indent = 0\\cm}");
			writer.println("\\header{tagline = \"\"}\n\n\\score{\n << \n");
			writer.println("\\absolute {\n"+
				"\\override Score.BarNumber.break-visibility = ##(#f #f #f)"
				+ dataset.get(0).toLilypond()+"\n}");
			writer.println("\n>>\n\\midi{}\n\\layout{}\n}");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
			System.out.println("I guess we give up now...");
		}
	}

	public static Double rank(Melody m){
		double rank = 0;

		for(int i = 0; i < m.melody.size(); i++){
			int cur = m.melody.get(i);
			if(cur<0){cur = 0;}
			//if note is "do" to start
			//RANK_STARTDO
			if(cur%12 == 0 && i == 0){
				rank +=RANK_STARTDO;
			}
			//if note is "do" to finish
			//RANK_ENDDO
			if(cur%12 == 0 && i == m.melody.size()-1){
				rank +=RANK_ENDDO;
			}

			//if note is "ti" or "ray" second to last
			//RANK_SECOND_TO_LAST
			if((cur%12 == 2 || cur%12==11) && i == m.melody.size()-2){
				rank +=RANK_SECOND_TO_LAST;
			}

			//if note is part of the chord on strong beat
			//RANK_CHORD_TONE_STRONG
			int chordIndex = (i/16)%4;
			if(i%4 == 0){
				if(CHORDS[chordIndex][cur%12]){
					rank += RANK_CHORD_TONE_STRONG;
				}
			}
			//if the note is part of the chord at all
			//RANK_CHORD_TONE_GEN
			if(CHORDS[chordIndex][cur%12]){
				rank += RANK_CHORD_TONE_GEN;
			}

			//if it is diatonic / part of the scale.
			//RANK_DIATONIC_EVER
			if(SCALE[cur%12]){
				rank+= RANK_DIATONIC_EVER;
			}
			
			//melodic leaps and stepwise motion
			int pvi = prevNote(i,m);
			if(pvi !=-1){	//comparisons to previous notes
				//if note is stepwise from one before

				//RANK_STEPWISE
				if(Math.abs(cur-m.melody.get(pvi)) <3){rank += RANK_STEPWISE;}
				else{
					for(int j = 0; j<chordTones[chordIndex].length; j++) {
						if(cur%12 == chordTones[chordIndex][j]){
							//RANK_CHORDAL_LEAP
							rank+= RANK_CHORDAL_LEAP;
						}
					}
				}
				//if note is a leap from one before
				//RANK_PUNISH_R1
				if(Math.abs(cur-m.melody.get(pvi)) > 4){rank -= RANK_LEAP_PUNISH_R1;}

				//if note is a huge from one before
				//RANK_PUNISH_R2
				if(Math.abs(cur-m.melody.get(pvi)) >8){rank -= RANK_LEAP_PUNISH_R2;}

				//if note is a huge from one before
				//RANK_PUNISH_R3
				if(Math.abs(cur-m.melody.get(pvi)) >11){rank -= RANK_LEAP_PUNISH_R3;}

				//if note is a huge from one before
				//RANK_PUNISH_R4
				if(Math.abs(cur-m.melody.get(pvi)) >14){rank -= RANK_LEAP_PUNISH_R4;}

				//if note is a TRITONE D:
				//RANK_PUNISH_TRI
				if(Math.abs(cur-m.melody.get(pvi))%12==6){rank -= RANK_LEAP_PUNISH_TRI;}
			
			}
			//now all the rhythm
			if(i%4 == 0){
				if(i!=0){
					//if note changes on a new beat
					//RANK_CHANGE_ON_STRONG
					if(cur != m.melody.get(i-1)){rank += RANK_CHANGE_ON_STRONG;}

					//if note changes from the previous strong beat
					//RANK_CHANGE_FROM_PREV_STRONG
					if(cur != m.melody.get(i-4)){rank += RANK_CHANGE_FROM_PREV_STRONG;}
				}
				if(i!=m.melody.size()-1){
					//if rhythm is 4th
					//RANK_QUARTER_NOTE_REWARD
					if(m.melody.get(i)==m.melody.get(i+1) &&
						m.melody.get(i+1)==m.melody.get(i+2) &&
						m.melody.get(i+2)==m.melody.get(i+3)){
						rank+=RANK_QUARTER_NOTE_REWARD;
					}
					//if rhythm is 8th 8th
					//RANK_DOUBLE_EIGHT_REWARD
					if(m.melody.get(i)==m.melody.get(i+1) &&
						m.melody.get(i+1)!=m.melody.get(i+2) &&
						m.melody.get(i+2)==m.melody.get(i+3)){
						rank+=RANK_DOUBLE_EIGHT_REWARD;
					}				
				}
			}
			//if note is in a reasonable range (g3 - e5)
			//RANK_RANGE_REWARD
			if(cur >= lowestNote && cur <= highestNote) {rank += 0;}else{rank-=RANK_RANGE_REWARD;}
		}
		return rank;
	}

	private static int prevNote(int index, Melody m){
		int ret = index;
		while(ret>0){
			ret--;
			if(m.melody.get(ret)!=m.melody.get(index)){
				return ret;
			}
		}
		return -1;
	}

	private static void vitals(int index){
		double average = 0;
		double max = Integer.MIN_VALUE;
		for(int i = 0; i < DATASET_SIZE; i ++){
			double cur = dataset.get(i).fitness;
			average += cur;
			if(cur>max){
				max = cur;
			}
		}
		average = average / DATASET_SIZE;
		log+= ", "+max;
	}
}
