package parser.enums;

import pacman.game.Game;
import parser.function.Function;
import parser.operators.NumericOperator;

import java.util.ArrayList;

public enum ConditionType {
	
	bool {
		public boolean evaluate(ArrayList<Function> functionList, int number, NumericOperator numberOp, Game g) {
			return (boolean) functionList.get(0).executeFunction(g);
		}
	}, func_num {
		public boolean evaluate(ArrayList<Function> functionList, int number, NumericOperator numberOp, Game g) {
			int num1 = (int) functionList.get(0).executeFunction(g);
			return numberOp.operate(num1, number);
			
		}
	}, num_func {
		public boolean evaluate(ArrayList<Function> functionList, int number, NumericOperator numberOp, Game g) {
			int num1 = (int) functionList.get(0).executeFunction(g);
			return numberOp.operate(number, num1);
			
		}
	}, func_func {
		public boolean evaluate(ArrayList<Function> functionList, int number, NumericOperator numberOp, Game g) {

			int num1 = (int) functionList.get(0).executeFunction(g);
			int num2 = (int) functionList.get(1).executeFunction(g);
			return numberOp.operate(num1, num2);
			
		}
	};

	public abstract boolean evaluate(ArrayList<Function> functionList, int number, NumericOperator numberOp, Game g);
	
	
}
