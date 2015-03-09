package net.indiespot.lmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.indiespot.lmc.LMC.Processor.State;

public class LMC {

	public static void main(String[] args) throws IOException, InterruptedException {
		{
			Compiler comp = new Compiler(true);
			comp.parse(LMC.class.getResourceAsStream("lmc_multiply.txt"));

			Processor proc = new Processor(comp.is16bit);
			comp.link(proc);
			// Arrays.fill(proc.breakpoints, true);
			// core.breakpoints[3] = true;

			int procFreq = 100;
			int gameFreq = 5;
			outer: while (true) {
				for (int i = 0; i < procFreq / gameFreq; i++) {
					System.out.println("i=" + i);
					State state = proc.tick();
					if (state == State.HALTED) {
						break outer;
					}
					if (state == State.IDLE) {
						System.out.println("-- idle");
						break;
					}
					if (state == State.CONTINUE) {
						continue;
					}
					if (state == State.BREAK_POINT) {
						proc.dump();
						Thread.sleep(500);
						continue;
					}
					throw new IllegalStateException();
				}
			}
			System.out.println(Arrays.toString(proc.out));

			// System.out.println(proc.mem[comp.resolve("A")]);
			// System.out.println(proc.mem[comp.resolve("B")]);
		}
	}

	static Map<String, Integer> mnemonic2instruction = new HashMap<>();
	static {
		mnemonic2instruction.put("DAT", -1);

		mnemonic2instruction.put("HLT", 0);

		mnemonic2instruction.put("ADD", 1);
		mnemonic2instruction.put("SUB", 2);
		mnemonic2instruction.put("STA", 3);
		mnemonic2instruction.put("DEC", 4);
		mnemonic2instruction.put("LDA", 5);
		mnemonic2instruction.put("BRA", 6);
		mnemonic2instruction.put("BRZ", 7);
		mnemonic2instruction.put("BRP", 8);

		mnemonic2instruction.put("RET", 9);
		mnemonic2instruction.put("INP", 9);
		mnemonic2instruction.put("OUT", 9);

		mnemonic2instruction.put("MUL", 10);
		mnemonic2instruction.put("DIV", 11);
		mnemonic2instruction.put("REM", 12);
		mnemonic2instruction.put("PSH", 13);
		mnemonic2instruction.put("POP", 14);
		mnemonic2instruction.put("JSR", 15);
	}

	static class Compiler {
		static class Code {
			int debugLineNr;
			int offset;
			String label, mnemonic, variable;

			@Override
			public String toString() {
				return "L" + debugLineNr + " " + label + " " + mnemonic + " " + variable;
			}
		}

		final boolean is16bit;
		int lineCounter;
		List<Code> codes = new ArrayList<>();

		public Compiler(boolean is16bit) {
			this.is16bit = is16bit;
		}

		public void parse(String lines) {
			for (String line : lines.split("\\r\\n|\\r|\\n")) {
				this.parseLine(line);
			}
		}

		public void parse(InputStream is) throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			for (String line; (line = br.readLine()) != null;) {
				this.parse(line);
			}
		}

		public void parseLine(String line) {
			lineCounter++;
			System.out.println("L" + lineCounter + ": " + line);

			String[] parts = line.replaceFirst("\\/\\/.*", "").trim().split("\\s+");
			if (parts.length == 0 || (parts.length == 1 && parts[0].isEmpty()))
				return;

			int io = -1;
			for (int i = 0; i < parts.length; i++) {
				if (mnemonic2instruction.containsKey(parts[i])) {
					if (io != -1)
						throw new IllegalStateException("multiple mnemonics");
					else
						io = i;
				}
			}
			if (io == -1)
				throw new IllegalStateException("missing mnemonic");

			Code code = new Code();
			code.debugLineNr = lineCounter;
			code.offset = (io - 2 >= 0) ? parseValue(parts[io - 2], (is16bit ? 0x100 : 100) - 1) : -1;
			code.label = (io - 1 >= 0) ? parts[io - 1] : null;
			code.mnemonic = parts[io];
			code.variable = (io + 1 < parts.length) ? parts[io + 1] : null;

			codes.add(code);
		}

		private boolean isValue(String s) {
			return s.matches("^[0-9]+$") || s.matches("^0x[0-9a-fA-F]+$");
		}

		public int resolve(String variable) {
			for (int io = 0; io < codes.size(); io++)
				if (variable.equals(codes.get(io).label))
					if (codes.get(io).offset != -1)
						return codes.get(io).offset;
			throw new IllegalStateException("undefined variable: '" + variable + "'");
		}

		private int parseValue(String value, int max) {
			int val;
			if (value.startsWith("0x"))
				val = Integer.parseInt(value.substring(2), 16);
			else
				val = Integer.parseInt(value, 10);

			if (val < 0 || val > max)
				throw new IllegalStateException("value out of bounds: '" + value + "'");
			return val;
		}

		private int checkAddress(int addr) {
			if (addr < 0 || addr > (is16bit ? 0x100 : 100) - 1)
				throw new IllegalStateException("address out of bounds: '" + addr + "'");
			return addr;
		}

		public void link(Processor proc) {
			if (proc.is16bit != this.is16bit)
				throw new IllegalStateException();

			proc.pc = 0;
			proc.acc = 0;
			proc.overflow = false;

			Arrays.fill(proc.inp, 0);
			Arrays.fill(proc.out, 0);
			Arrays.fill(proc.mem, -1);
			Arrays.fill(proc.breakpoints, false);

			Set<String> variables = new HashSet<>();
			for (Code code : codes)
				if (code.label != null)
					if (!variables.add(code.label))
						throw new IllegalStateException("duplicate variable definition: '" + code.label + "'");

			int offset = 0;
			for (Code code : codes)
				if (code.offset == -1)
					code.offset = offset++;

			for (Code code : codes) {
				int addr;
				if (code.variable == null)
					addr = 0;
				else if (isValue(code.variable))
					addr = parseValue(code.variable, (is16bit ? 0x1000 : 1000) - 1);
				else
					addr = this.resolve(code.variable);

				int cmd = -1;
				if (!is16bit && code.mnemonic.equals("DEC"))
					throw new IllegalStateException();
				else if (code.mnemonic.equals("DAT"))
					cmd = addr;
				else if (is16bit && code.mnemonic.equals("RET"))
					if (code.variable == null)
						cmd = mnemonic2instruction.get(code.mnemonic) * (is16bit ? 0x100 : 100) + 0;
					else
						throw new IllegalStateException();
				else if (code.mnemonic.equals("INP"))
					cmd = mnemonic2instruction.get(code.mnemonic) * (is16bit ? 0x100 : 100) + 1 + addr * 2;
				else if (code.mnemonic.equals("OUT"))
					cmd = mnemonic2instruction.get(code.mnemonic) * (is16bit ? 0x100 : 100) + 2 + addr * 2;
				else if (mnemonic2instruction.containsKey(code.mnemonic))
					cmd = mnemonic2instruction.get(code.mnemonic) * (is16bit ? 0x100 : 100) + checkAddress(addr);
				if (cmd == -1)
					throw new IllegalStateException();

				if (proc.mem[code.offset] != -1)
					throw new IllegalStateException("overwritten instruction at address " + code.offset);
				proc.mem[code.offset] = cmd;

				System.out.println("@" + code.offset + ": " + (is16bit ? "0x" + Integer.toHexString(cmd) : cmd) + " // " + code);
			}

			for (int i = 0; i < proc.mem.length; i++)
				if (proc.mem[i] == -1)
					proc.mem[i] = 0;
		}
	}

	static class Processor {
		boolean acc_clamp = true;
		boolean brz_use_neg = true;
		boolean with_dec_cmd = true;

		int pc, sp;
		int[] inp;
		int[] out;
		int acc;
		boolean overflow;

		final boolean is16bit;
		final int cells, range, rangeM1;
		int[] mem;
		boolean[] breakpoints;

		public Processor(boolean is16Bit) {
			this.is16bit = is16Bit;
			inp = new int[is16Bit ? 16 : 1];
			out = new int[is16Bit ? 16 : 1];
			cells = is16bit ? 0x100 : 100;
			range = is16bit ? 0x1000 : 1000;
			rangeM1 = range - 1;
			pc = 0;
			sp = cells;
			mem = new int[cells];
			breakpoints = new boolean[cells];
		}

		public void dump() {
			System.out.println("pc=" + pc + ", acc=" + acc + ", overflow=" + overflow + ", inp=" + inp + ", out=" + out);
		}

		static enum State {
			CONTINUE, BREAK_POINT, IDLE, HALTED
		}

		public State tick() {
			int instruction = mem[pc++];
			int addr = instruction % cells;

			switch (instruction / cells) {
			case 0: // HLT
				if (addr == 0)
					return State.HALTED;
				else if (addr == 1)
					return State.IDLE;
				else
					throw new IllegalStateException("invalid instruction: " + instruction);
			case 1: // ADD
				acc = this.normalize(acc + mem[addr]);
				break;
			case 2: // SUB
				acc = this.normalize(acc - mem[addr]);
				break;
			case 3: // STA
				if (acc < 0 || acc > rangeM1)
					throw new RuntimeException("corrupt accumulator: " + acc);
				mem[addr] = acc;
				break;
			case 4: // DEC
				if (!with_dec_cmd)
					throw new IllegalStateException("invalid instruction: " + instruction);
				mem[addr] = (mem[addr] + rangeM1) % range;
				overflow = (mem[addr] == rangeM1);
				break;
			case 5: // LDA
				acc = mem[addr];
				break;
			case 6: // BRA
				pc = addr;
				break;
			case 7: // BRZ
				if ((!brz_use_neg || !overflow) && acc == 0)
					pc = addr;
				break;
			case 8: // BRP
				if (!overflow)
					pc = addr;
				break;
			case 9:
				if (addr == 0) {
					// RET
					pc = mem[sp++];
				} else if (addr % 2 == 1 && (addr - 0) / 2 < inp.length) {
					// INP
					acc = inp[(addr - 0) / 2];
				} else if (addr % 2 == 0 && (addr - 1) / 2 < out.length) {
					// OUT
					out[(addr - 1) / 2] = acc;
				} else {
					throw new IllegalStateException("invalid instruction: " + instruction);
				}
				break;
			case 10: // MUL
				acc = this.normalize(acc * mem[addr]);
				break;
			case 11: // DIV
				acc = this.normalize(acc / mem[addr]);
				break;
			case 12: // REM
				acc = this.normalize(acc % mem[addr]);
				break;
			case 13: // PSH
				mem[--sp] = mem[addr];
				break;
			case 14: // POP
				mem[addr] = mem[sp++];
				break;
			case 15: // JSR
				mem[--sp] = pc;
				pc = addr;
				break;
			default:
				throw new IllegalStateException("invalid instruction: " + instruction);
			}

			if (breakpoints[pc])
				return State.BREAK_POINT;
			return State.CONTINUE;
		}

		private int normalize(int value) {
			overflow = false;
			if (value < 0) {
				if (acc_clamp)
					value = 0;
				else
					value = ((value % range) + range) % range;
				overflow = true;
			} else if (value > rangeM1) {
				if (acc_clamp)
					value = rangeM1;
				else
					value = ((value % range) + range) % range;
				overflow = true;
			}
			return value;
		}
	}
}
