
package engine;

import java.util.*;

import engine.things.Player;
import engine.things.Effect;
import engine.things.Entity;
import engine.things.Object;
import engine.words.Verb;
import engine.words.Word;

import engine.Terminal;

public class Engine {
	public Player protag;

	public ArrayList<Room> rooms;// can be accessed by verbs
	private ArrayList<Word> vocabulary;
	public ArrayList<Object> objectQueue = new ArrayList<Object>();
	Random rand = new Random();

	public Engine() {
		protag = new Player(0, 0);
		protag.setHealth(100);

		rooms = new ArrayList<Room>();
		
		RoomGen.gen(rooms, objectQueue);
		
		ArrayList<Object> references = new ArrayList<Object>();
		for (Room r : rooms) {
			Iterator<Object> it = r.objects.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj.reference != null) {
					references.add(obj.reference);
				}
				if (!obj.container.isEmpty()) {
					references.addAll(obj.container);
				}
			}
		}
		for (Room r : rooms) {
			r.objects.addAll(references);
		}

		vocabulary = new ArrayList<Word>();
	}

	public void addRoom(Room r) {
		rooms.add(r);
	}

	public void addWord(Word v) {
		vocabulary.add(v);
	}

	public static Object Consumable(String accessor, String descriptor, String inspection, int consumability) {
		Object o = new Object(accessor, descriptor, inspection);
		o.consumability = consumability;
		return o;
	}

	public String uRandOf(String[] s) {
		int x = rand.nextInt(s.length);

		// Convert 'a's to 'an's
		for (int i = 2; i < s[x].length(); i++) {
			ArrayList<Character> vowels = new ArrayList<Character>(Arrays.asList('a', 'e', 'i', 'o', 'u'));
			if (vowels.contains(s[x].charAt(i)) && s[x].charAt(i - 2) == 'a' && s[x].charAt(i - 1) == ' '
					&& s[x].charAt(i - 3) == ' ') {
				s[x] = s[x].substring(0, i - 2) + "an" + s[x].substring(i - 1, s[x].length());
			}
		}
		return s[x].substring(0, 1).toUpperCase() + s[x].substring(1, s[x].length());
	}

	public String lRandOf(String[] s) {
		int x = rand.nextInt(s.length);
		// Convert 'a's to 'an's
		for (int i = 2; i < s[x].length(); i++) {
			ArrayList<Character> vowels = new ArrayList<Character>(Arrays.asList('a', 'e', 'i', 'o', 'u'));
			if (vowels.contains(s[x].charAt(i)) && s[x].charAt(i - 2) == 'a' && s[x].charAt(i - 1) == ' '
					&& s[x].charAt(i - 3) == ' ') {
				s[x] = s[x].substring(0, i - 2) + "an" + s[x].substring(i - 1, s[x].length());
			}
		}
		return s[x].toLowerCase();
	}

	public void updatePlayerState() {
		if (protag.hunger++ > 50) {
			if (protag.hunger > 80) {
				Terminal.println(
						"Intuition tells you that if you had eaten something, perhaps you wouldn't be so hungry.");
			} else {
				Terminal.println("Intuition tells you that you might want to eat some food.");
			}
		}
		if (protag.thirst++ > 50) {
			if (protag.thirst > 80) {
				Terminal.println("Intuition tells you that your thirst cannot be quenched by drinking air.");
			} else {
				Terminal.println("Intuition tells you that you might want to drink something.");
			}
		}
		protag.health += protag.health < protag.maxHealth ? 1 : 0;
	}

	public void update() {
		String userText;
		ArrayList<String> words;

		while (true) {// repeats until valid command

			objectQueue.clear();
			boolean foundRoom = false;
			for (Room r : rooms) {
				if (r.toString().equals(protag.toString())) {
					protag.currentRoom = r;
					foundRoom = true;
				}
			}

			Terminal.println(protag.health > 90 ? "You are feeling fine."
					: protag.health > 50 ? "You are feeling slightly injured."
							: protag.health > 0
									? "You think that you might have some injuries, but you've forgotten where."
									: "You feel slightly dead, but you aren't sure.");

			if (foundRoom)
				Terminal.println(protag.currentRoom.description);
			else
				Terminal.println("Currently not in any room!");

			for (Object o : protag.currentRoom.objects) {
				if (o.health != null && o.health <= 0) {
					for (Object obj : o.container) {
						obj.reference = protag.currentRoom.floor;
						obj.description = lRandOf(new String[] { "lying", "sitting", "resting" }) + " on";

					}
					o.container.clear();
				}
			}
			int x1 = 0;
			int x2 = 0;
			Iterator<Object> objectIt = protag.currentRoom.objects.iterator();
			while (objectIt.hasNext()) {
				Object o = objectIt.next();
				if (o.alive && o.health <= 0) {
					if (o.getClass().getSimpleName().equals("Entity")) {
						Entity e = (Entity) o;
						e.death.accept(this);
						for (Object obj : e.inventory) {
							Object ref = new Object("the [floor]", obj, null);
							ref.abstractNoun();
							obj.reference = ref;
							objectQueue.add(obj);
						}
						objectIt.remove();
					}
				}
			}
			protag.currentRoom.objects.addAll(objectQueue);
			for (int i = 0; i < protag.currentRoom.objects.size(); i++) {
				Object o = protag.currentRoom.objects.get(i);
				String compSub = o.compSub;
				if (o.health != null && o.health < o.maxHealth) {
					int p = (int) (((float) o.health / (float) o.maxHealth) * 4);
					switch (o.injury) {
					case crumples:
						compSub = (p == 3 ? "dented " : p == 2 ? "bent " : p == 1 ? "crumpled-up " : "crushed ")
								+ o.accessor;
						break;
					case shatters:
						compSub = (p == 3 ? "fractured " : p > 0 ? "cracked " : "shattered ") + o.accessor;
						break;
					case squishes:
						compSub = (p == 3 ? "bruised " : p == 2 ? "squashed " : p == 1 ? "compressed " : "trampled ")
								+ o.accessor;
						break;
					case bruises:
						compSub = (p == 3 ? "bruised " : p == 2 ? "damaged " : p == 1 ? "beaten-up " : "pulverized ")
								+ o.accessor;
						break;
					}
				}
				if (protag.hunger > 0) {
					if (rand.nextInt(101 - protag.hunger) < 5 && o.reference != null) {
						compSub = lRandOf(new String[] { "possibly edible", "juicy and tender", "appetizing",
								"delicious-looking", "scrumptious" }) + " " + compSub;
					}
				}

				int n = i + 1;
				String s = null;
				while (s == null) {
					try {
						s = protag.currentRoom.objects.get(n).reference.accessor;
						if (s.equals(o.reference.accessor)) {
							x1 = 2;
						}
						break;
					} catch (NullPointerException e) {

					} catch (IndexOutOfBoundsException e) {
						break;
					}
					n++;
				}
				n = i - 1;
				s = null;
				while (s == null) {
					try {
						s = protag.currentRoom.objects.get(n).reference.accessor;
						if (s.equals(o.reference.accessor)) {
							x2 = 1;
						}
						break;
					} catch (NullPointerException e) {

					} catch (IndexOutOfBoundsException e) {
						break;
					}
					n--;
				}

				try {
					Object r = o.reference;
					if (x1 == 1) {
						if (x2 == 0) {
							Terminal.print(lRandOf(
									new String[] { " as well as a " + compSub + " " + o.description + " " + r.compSub,
											" and a " + compSub + " " + o.description + " " + r.compSub }));
						} else {
							Terminal.print(", and a " + compSub + " " + o.description + " " + r.compSub);
						}
					} else if (x1 == 2) {
						if (x2 == 0) {
							Terminal.print(
									uRandOf(new String[] { "there is a " + compSub, "You notice a " + compSub }));
						} else {
							Terminal.print(", a " + compSub);
						}
					} else {
						Terminal.print(
								uRandOf(new String[] { "there is a " + compSub + " " + o.description + " " + r.compSub,
										o.description + " " + r.compSub + ", there is a " + compSub,
										"You notice a " + compSub + " " + o.description + " " + r.compSub }));
					}
					if (x1 > 0) {
						x1--;
					}
					if (x1 == 0) {
						x2 = 0;
						Terminal.print(". ");
					}
				} catch (NullPointerException e) {

				}

			}
			Terminal.print("\n");

			userText = Terminal.readln();
			userText = userText.toLowerCase();
			words = new ArrayList<String>();

			String[] s = userText.split(" ");
			for (String str : s) {
				words.add(str);// user text goes to array of words
			}
			if (words.size() != 2) {
				Terminal.println("All commands must be 2 words.");
				continue;
			}

			Word w0 = null;
			Word w1 = null;
			Object o1 = null;
			boolean found = false;
			boolean foundObject = false;

			for (Word w : vocabulary) {
				if (w.checkWord(words.get(0))) {
					w0 = w;
					found = true;
				}
			} // finds word in array

			if (!found) {
				Terminal.println("I don't know what '" + words.get(0) + "' means.");
				continue;
			}

			if (w0.getClass() != Verb.class) {
				Terminal.println("Commands always start with a verb.");
				continue;
			}

			found = false;

			for (Word w : vocabulary) {
				if (w.checkWord(words.get(1))) {
					w1 = w;
					found = true;
				}
			}

			for (Object o : protag.currentRoom.objects) {
				if (o.accessor.equals(words.get(1))) {
					o1 = o;
					foundObject = true;
				}
			}

			for (Object o : protag.inventory) {
				if (o.accessor.equals(words.get(1))) {
					o1 = o;
					foundObject = true;
				}
			}

			if (!found && !foundObject) {
				Terminal.println("I don't know what '" + words.get(1) + "' means.");
				continue;
			}

			if (found && !foundObject) {
				if (w1.getClass() == Verb.class) {
					Terminal.println("Commands never end with a verb.");
					continue;
				}
			}

			if (found) {
				w0.perform(w1, this);// fills out word's function
			} else if (foundObject) {
				w0.perform(o1, this);
			}

			updatePlayerState();
			Iterator<Effect> effectIt = protag.effects.iterator();
			while (effectIt.hasNext()) {
				Effect e = effectIt.next();
				e.affect(protag);
				if (e.lifetime == 0) {
					effectIt.remove();
				}
			}
			break;
		}
	}
}
