package com.varundev.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.varundev.calculator.ui.theme.*
import java.util.Stack

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CalculatorApp()
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun  CalculatorApp() {
        var isDarkMode by remember { mutableStateOf(false) }

        val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme

        MaterialTheme(colorScheme = colorScheme) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = "Calculator")
                        },
                        actions = {
                            IconButton(onClick = { isDarkMode = !isDarkMode }) {
                                val iconImage = if (isDarkMode) {
                                    painterResource(id = R.drawable.light_mode)
                                } else {
                                    painterResource(id = R.drawable.dark_mode)
                                }
                                Image(painter = iconImage,
                                    contentDescription = "Toggle Theme",
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                },
                content = {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val isWideScreen = maxWidth > 600.dp

                        var input by remember { mutableStateOf("") }
                        var result by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier.padding(16.dp).widthIn(
                                max = if (isWideScreen) 600.dp else maxWidth
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DisplayScreen(input, result)

                            CalculationButton { action ->
                                when (action) {
                                    "=" -> result = evaluateExpression(input)
                                    "AC" -> {
                                        input = ""
                                        result = ""
                                    }
                                    "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                    "%" -> if (input.isNotEmpty()) input = (input.toDoubleOrNull()?.div(100))?.toString() ?: input
                                    else -> input += action
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    @Composable
    fun DisplayScreen(input: String, result: String) {

        Column(modifier = Modifier.fillMaxWidth()
            .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = input,
                fontSize = 24.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            Text(
                text = result,
                fontSize = 32.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }

    @Composable
    fun CalculationButton(onClick: (String) -> Unit) {
        val buttonLabels = listOf(
            listOf("AC", "⌫", "00", "÷"),
            listOf("7", "8", "9", "x"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("%", "0", ".", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            buttonLabels.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { buttonLabel ->
                        Button(
                            onClick = { onClick(buttonLabel) },
                            colors = ButtonColors(
                                containerColor = if (buttonLabel != "=") MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.surfaceContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                disabledContentColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            Text(text = buttonLabel,
                                textAlign = TextAlign.Center,
                                fontSize = if (buttonLabel != "=") 20.sp else 34.sp,
                                color = MaterialTheme.colorScheme.surfaceContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    private fun evaluateExpression(expression: String): String {
        return try {
            val sanitizedValue = expression.replace("x", "*").replace("÷", "/")
            val returnValue = evaluate(sanitizedValue)
            returnValue.toString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun evaluate(expression: String): Double {
        val tokens = expression.toCharArray()
        val values = Stack<Double>()    // Stack for numbers
        val operators = Stack<Char>()  // Stack for operators

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            when {
                token.isWhitespace() -> {
                    i++
                }
                token.isDigit() -> {
                    val sb = StringBuilder()
                    while (i < tokens.size && (tokens[i].isDigit() || tokens[i] == '.')) {
                        sb.append(tokens[i++])
                    }
                    values.push(sb.toString().toDouble())
                }
                token == '(' -> {
                    operators.push(token)
                    i++
                }
                token == ')' -> {
                    while (operators.isNotEmpty() && operators.peek() != '(') {
                        values.push(applyOperator(operators.pop(), values.pop(), values.pop()))
                    }
                    operators.pop()
                    i++
                }
                token in "+-*/" -> {
                    while (operators.isNotEmpty() && hasPrecedence(token, operators.peek())) {
                        values.push(applyOperator(operators.pop(), values.pop(), values.pop()))
                    }
                    operators.push(token)
                    i++
                }
                else -> throw IllegalArgumentException("Invalid character in expression: $token")
            }
        }

        while (operators.isNotEmpty()) {
            values.push(applyOperator(operators.pop(), values.pop(), values.pop()))
        }

        return values.pop()
    }

    private fun applyOperator(op: Char, b: Double, a: Double): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw ArithmeticException("Cannot divide by zero")
                a / b
            }
            else -> throw IllegalArgumentException("Unsupported operator: $op")
        }
    }

    private fun hasPrecedence(op1: Char, op2: Char): Boolean {
        return if (op2 == '(' || op2 == ')') false
        else (op1 != '*' && op1 != '/') || (op2 != '+' && op2 != '-')
    }
}
