# ggplot2 charts (DV weeks 4 and 5). Install once: install.packages("ggplot2")
library(ggplot2)

args <- commandArgs(trailingOnly = TRUE)
path <- if (length(args) >= 1) args[1] else "data/vault_stats.csv"
data <- read.csv(path)
data$strength <- factor(data$strength, levels = c("Weak", "Medium", "Strong"))
colours <- c(Weak = "#C62828", Medium = "#E65100", Strong = "#2E7D32")
dir.create("output", showWarnings = FALSE)

p1 <- ggplot(data, aes(x = category, fill = strength)) +
  geom_bar() +
  scale_fill_manual(values = colours) +
  labs(title = "Password strength by category", x = "Category", y = "Entries")
ggsave("output/gg_strength_by_category.png", p1, width = 8, height = 5)

p2 <- ggplot(data, aes(x = length, fill = strength)) +
  geom_histogram(binwidth = 2, colour = "white") +
  scale_fill_manual(values = colours) +
  facet_wrap(~category) +
  labs(title = "Password length, one panel per category", x = "Length", y = "Entries")
ggsave("output/gg_length_facets.png", p2, width = 10, height = 6)

reuse <- as.data.frame(table(Reused = ifelse(as.logical(data$is_reused), "Reused", "Unique")))
p3 <- ggplot(reuse, aes(x = "", y = Freq, fill = Reused)) +
  geom_col(width = 1) +
  coord_polar(theta = "y") +
  theme_void() +
  labs(title = "Reused vs unique passwords")
ggsave("output/gg_reuse_pie.png", p3, width = 5, height = 5)

p4 <- ggplot(data, aes(x = length, y = age_days, colour = strength)) +
  geom_point(size = 2) +
  scale_colour_manual(values = colours) +
  labs(title = "Length vs age", x = "Length", y = "Age (days)")
ggsave("output/gg_length_vs_age.png", p4, width = 7, height = 5)

cat("Saved 4 charts in output/\n")
