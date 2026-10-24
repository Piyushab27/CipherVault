# DV weeks 9 to 11: weak % against a target, mean and median length, a simple control chart.
library(ggplot2)

args <- commandArgs(trailingOnly = TRUE)
path <- if (length(args) >= 1) args[1] else "data/vault_stats.csv"
data <- read.csv(path)
dir.create("output", showWarnings = FALSE)

target <- 10  # we want at most 10% weak passwords
by_cat <- aggregate(strength == "Weak" ~ category, data = data, FUN = function(x) 100 * mean(x))
names(by_cat)[2] <- "pct_weak"

p1 <- ggplot(by_cat, aes(x = category, y = pct_weak)) +
  geom_col(fill = "steelblue") +
  geom_hline(yintercept = target, colour = "red", linetype = "dashed") +
  labs(title = "% weak passwords against the 10% target", x = "Category", y = "% weak")
ggsave("output/weak_vs_target.png", p1, width = 8, height = 5)

cat("Mean length:  ", round(mean(data$length), 2), "\n")
cat("Median length:", median(data$length), "\n")

# Control chart: length of entries in the order they were exported, with mean +/- 3 standard deviations
data$entry <- seq_len(nrow(data))
m <- mean(data$length); s <- sd(data$length)
p2 <- ggplot(data, aes(x = entry, y = length)) +
  geom_line(colour = "grey50") + geom_point() +
  geom_hline(yintercept = m, colour = "darkgreen") +
  geom_hline(yintercept = c(m - 3 * s, m + 3 * s), colour = "red", linetype = "dashed") +
  labs(title = "Control chart: password length", x = "Entry", y = "Length")
ggsave("output/control_chart.png", p2, width = 8, height = 5)

p3 <- ggplot(data, aes(x = category, fill = factor(strength, levels = c("Weak", "Medium", "Strong")))) +
  geom_bar(position = "fill") +
  scale_y_continuous(labels = scales::percent) +
  labs(title = "Strength mix by category", x = "Category", y = "Share", fill = "Strength")
ggsave("output/stacked_strength.png", p3, width = 8, height = 5)
