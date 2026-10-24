# Base R charts (DV weeks 1 to 3). Run from the project folder: Rscript r/01_base_r_charts.R
# Uses data/vault_stats.csv by default; pass your own export as the first argument.
args <- commandArgs(trailingOnly = TRUE)
path <- if (length(args) >= 1) args[1] else "data/vault_stats.csv"
data <- read.csv(path)
data$strength <- factor(data$strength, levels = c("Weak", "Medium", "Strong"))

dir.create("output", showWarnings = FALSE)
png("output/base_r_charts.png", width = 1200, height = 900)
par(mfrow = c(2, 2))

barplot(table(data$category), main = "Entries per category", xlab = "Category", ylab = "Entries",
        col = "steelblue", las = 2)
hist(data$length, main = "Password length", xlab = "Length", col = "grey70", breaks = 10)
boxplot(length ~ strength, data = data, main = "Length by strength", xlab = "Strength", ylab = "Length",
        col = c("tomato", "orange", "seagreen"))
plot(data$length, data$age_days, main = "Length vs age", xlab = "Length", ylab = "Age (days)",
     pch = 19, col = c("tomato", "orange", "seagreen")[data$strength])
legend("topright", legend = levels(data$strength), col = c("tomato", "orange", "seagreen"), pch = 19)

dev.off()
cat("Saved output/base_r_charts.png\n")
