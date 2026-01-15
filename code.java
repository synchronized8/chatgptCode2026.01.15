import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class code {
    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    private static final Map<Character, Integer> PRECEDENCE = new HashMap<>();

    static {
        PRECEDENCE.put('+', 1);
        PRECEDENCE.put('-', 1);
        PRECEDENCE.put('*', 2);
        PRECEDENCE.put('/', 2);
    }

    public static void main(String[] args) {
        System.out.println("简易计算器\n支持 + - * / 和括号\n输入 exit 退出");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("请输入表达式: ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("再见!");
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            try {
                BigDecimal result = evaluate(input);
                System.out.println("结果: " + result.stripTrailingZeros().toPlainString());
            } catch (IllegalArgumentException ex) {
                System.out.println("错误: " + ex.getMessage());
            }
        }
    }

    private static BigDecimal evaluate(String expression) {
        String normalized = expression.replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("表达式为空");
        }
        Deque<BigDecimal> values = new ArrayDeque<>();
        Deque<Character> operators = new ArrayDeque<>();

        int index = 0;
        while (index < normalized.length()) {
            char current = normalized.charAt(index);
            if (current == '(') {
                operators.push(current);
                index++;
            } else if (current == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    applyOperator(values, operators.pop());
                }
                if (operators.isEmpty() || operators.peek() != '(') {
                    throw new IllegalArgumentException("括号不匹配");
                }
                operators.pop();
                index++;
            } else if (isOperator(current)) {
                if (isUnaryOperator(normalized, index)) {
                    int nextIndex = index + 1;
                    if (nextIndex >= normalized.length()) {
                        throw new IllegalArgumentException("一元运算符后缺少数字或括号");
                    }
                    if (normalized.charAt(nextIndex) == '(') {
                        values.push(BigDecimal.ZERO);
                        operators.push(current == '+' ? '+' : '-');
                        index++;
                        continue;
                    }
                    if (current == '+') {
                        NumberParseResult parseResult = parseNumber(normalized, nextIndex);
                        values.push(parseResult.number);
                        index = parseResult.nextIndex;
                        continue;
                    }
                    NumberParseResult parseResult = parseNumber(normalized, nextIndex);
                    values.push(parseResult.number.negate());
                    index = parseResult.nextIndex;
                    continue;
                }
                while (!operators.isEmpty() && operators.peek() != '(' &&
                        PRECEDENCE.get(operators.peek()) >= PRECEDENCE.get(current)) {
                    applyOperator(values, operators.pop());
                }
                operators.push(current);
                index++;
            } else if (Character.isDigit(current) || current == '.') {
                NumberParseResult parseResult = parseNumber(normalized, index);
                values.push(parseResult.number);
                index = parseResult.nextIndex;
            } else {
                throw new IllegalArgumentException("非法字符: " + current);
            }
        }

        while (!operators.isEmpty()) {
            char operator = operators.pop();
            if (operator == '(') {
                throw new IllegalArgumentException("括号不匹配");
            }
            applyOperator(values, operator);
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("表达式不合法");
        }
        return values.pop();
    }

    private static boolean isOperator(char value) {
        return PRECEDENCE.containsKey(value);
    }

    private static boolean isUnaryOperator(String input, int index) {
        char current = input.charAt(index);
        if (current != '+' && current != '-') {
            return false;
        }
        return index == 0 || input.charAt(index - 1) == '(' || isOperator(input.charAt(index - 1));
    }

    private static void applyOperator(Deque<BigDecimal> values, char operator) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("表达式不合法");
        }
        BigDecimal right = values.pop();
        BigDecimal left = values.pop();
        BigDecimal result;
        switch (operator) {
            case '+':
                result = left.add(right, MATH_CONTEXT);
                break;
            case '-':
                result = left.subtract(right, MATH_CONTEXT);
                break;
            case '*':
                result = left.multiply(right, MATH_CONTEXT);
                break;
            case '/':
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("除数不能为 0");
                }
                result = left.divide(right, MATH_CONTEXT);
                break;
            default:
                throw new IllegalArgumentException("未知运算符: " + operator);
        }
        values.push(result);
    }

    private static NumberParseResult parseNumber(String input, int startIndex) {
        int index = startIndex;
        boolean hasDot = false;
        while (index < input.length()) {
            char current = input.charAt(index);
            if (Character.isDigit(current)) {
                index++;
                continue;
            }
            if (current == '.') {
                if (hasDot) {
                    break;
                }
                hasDot = true;
                index++;
                continue;
            }
            break;
        }
        if (startIndex == index) {
            throw new IllegalArgumentException("数字解析失败");
        }
        BigDecimal number = new BigDecimal(input.substring(startIndex, index), MATH_CONTEXT);
        return new NumberParseResult(number, index);
    }

    private static class NumberParseResult {
        private final BigDecimal number;
        private final int nextIndex;

        private NumberParseResult(BigDecimal number, int nextIndex) {
            this.number = number;
            this.nextIndex = nextIndex;
        }
    }
}
