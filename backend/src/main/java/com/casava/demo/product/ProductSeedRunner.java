package com.casava.demo.product;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class ProductSeedRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ProductSeedRunner.class);

  private final ProductRepository productRepository;
  private final ProductExclusionRepository exclusionRepository;
  private final ProductFaqRepository faqRepository;

  public ProductSeedRunner(
      ProductRepository productRepository,
      ProductExclusionRepository exclusionRepository,
      ProductFaqRepository faqRepository) {
    this.productRepository = productRepository;
    this.exclusionRepository = exclusionRepository;
    this.faqRepository = faqRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    long existing = productRepository.count();
    if (existing > 0) {
      log.info(
          "Product catalog already seeded ({} products, {} exclusions, {} FAQs) — skipping seed",
          existing,
          exclusionRepository.count(),
          faqRepository.count());
      return;
    }

    log.info("Seeding Casava-shaped demo product catalog...");

    Product income = seedIncomeProtection();
    Product health = seedHealthCash();
    Product device = seedDeviceProtection();

    log.info(
        "Seeded product catalog: {} products (slugs: {}, {}, {}), {} exclusions, {} FAQs",
        productRepository.count(),
        income.getSlug(),
        health.getSlug(),
        device.getSlug(),
        exclusionRepository.count(),
        faqRepository.count());
  }

  private Product seedIncomeProtection() {
    Product product = new Product();
    product.setSlug("income-protection");
    product.setName("Income Protection");
    product.setTagline("Replace income when illness or injury keeps you from working");
    product.setMonthlyFrom(new BigDecimal("500"));
    product.setCoverHighlights(
        List.of(
            "From ₦500/month",
            "Up to ₦240,000 monthly benefit",
            "Up to 6 months of cover per claim",
            "Pays while you recover — not after you return to work"));
    product.setDescription(
        """
        DEMO DATA — not a live Casava policy wording.

        Income Protection helps replace a portion of your salary if a covered illness or injury \
        stops you from working. Plans start from ₦500 per month and can pay up to ₦240,000 per \
        month for up to 6 months while you recover.

        Benefits are designed for salaried and gig workers who need cashflow continuity during \
        medically certified time off. Waiting periods, occupational classes, and medical underwriting \
        may apply. This seed content is for the Ask Casa RAG demo only.
        """);
    product = productRepository.save(product);

    saveExclusion(
        product,
        "Pre-existing medical conditions disclosed (or that should have been disclosed) within the "
            + "lookback window are not covered.");
    saveExclusion(
        product,
        "Self-inflicted injury, intoxication, or participation in professional/extreme sports are excluded.");
    saveExclusion(
        product,
        "Unemployment, redundancy, or resignation without a covered medical reason does not trigger benefits.");
    saveExclusion(
        product,
        "Claims arising from war, civil unrest, or illegal activities are not covered.");

    saveFaq(
        product,
        "How much can Income Protection pay?",
        "Demo plans can pay up to ₦240,000 per month for up to 6 months, subject to your selected "
            + "cover level and underwriting. Figures are demo data.");
    saveFaq(
        product,
        "When do monthly benefits start?",
        "After a waiting period (demo default: 14–30 days) and once medical certification confirms "
            + "you cannot work due to a covered condition.");
    saveFaq(
        product,
        "Who is Income Protection for?",
        "People who rely on earned income — employees, freelancers, and gig workers — who want "
            + "cashflow support if illness or injury keeps them off work. Demo product only.");
    saveFaq(
        product,
        "Does it pay if I still work part-time?",
        "Partial-return scenarios may reduce the benefit. Exact rules depend on the policy schedule; "
            + "this demo seed does not replace underwriting guidance.");

    return product;
  }

  private Product seedHealthCash() {
    Product product = new Product();
    product.setSlug("health-cash");
    product.setName("Health Cash");
    product.setTagline("Cash support for hospital stays and emergency treatment");
    product.setMonthlyFrom(new BigDecimal("350"));
    product.setCoverHighlights(
        List.of(
            "From ₦350/month",
            "Up to ₦500,000 per covered incident",
            "Hospital cash for overnight stays",
            "Emergency treatment cash benefit"));
    product.setDescription(
        """
        DEMO DATA — not a live Casava policy wording.

        Health Cash pays a defined cash benefit when you are admitted overnight or receive covered \
        emergency treatment — helping with transport, lost wages, and out-of-pocket costs that medical \
        insurance may not fully cover.

        Plans start from ₦350 per month with incident benefits of up to ₦500,000 depending on the \
        selected tier. Payouts are cash to you, not direct settlement to the hospital. Seeded for \
        the Ask Casa RAG demo.
        """);
    product = productRepository.save(product);

    saveExclusion(
        product,
        "Routine outpatient visits, dental, optical, and wellness check-ups are not covered incidents.");
    saveExclusion(
        product,
        "Hospitalisation for maternity complications may require an optional rider; base demo cover excludes it.");
    saveExclusion(
        product,
        "Treatment outside Nigeria (or outside the listed partner network, where applicable) is excluded "
            + "unless pre-authorised.");
    saveExclusion(
        product,
        "Claims related to alcohol or drug abuse, or elective cosmetic procedures, are excluded.");

    saveFaq(
        product,
        "What is the maximum payout per incident?",
        "Demo Health Cash tiers pay up to ₦500,000 per covered incident. Limits and co-pays vary by plan.");
    saveFaq(
        product,
        "Is Health Cash the same as health insurance?",
        "No. Health Cash pays you a cash benefit for covered events; it does not replace comprehensive "
            + "medical insurance that settles hospital bills directly.");
    saveFaq(
        product,
        "How fast are claims paid?",
        "After approved documentation (admission notes / discharge summary), demo target turnaround is "
            + "a few business days. Not a live SLA.");
    saveFaq(
        product,
        "Can I use any hospital?",
        "Cash benefits are typically paid for admissions at recognised facilities. Network rules in this "
            + "demo are illustrative only.");

    return product;
  }

  private Product seedDeviceProtection() {
    Product product = new Product();
    product.setSlug("device-protection");
    product.setName("Device Protection");
    product.setTagline("Cover for phones, laptops, and tablets against theft, drop, and water damage");
    product.setMonthlyFrom(new BigDecimal("2500"));
    product.setCoverHighlights(
        List.of(
            "From ₦2,500/month",
            "Cover up to ₦1,500,000 device value",
            "Theft, accidental damage, and liquid damage",
            "Repair or replacement after approval"));
    product.setDescription(
        """
        DEMO DATA — not a live Casava policy wording.

        Device Protection covers eligible smartphones, laptops, and tablets against theft, accidental \
        drop damage, and liquid damage. Monthly premiums start from ₦2,500 with sum insured options \
        up to ₦1,500,000 depending on device category and valuation.

        Claims may result in approved repair or replacement of a like-for-like device. Proof of \
        ownership, police reports for theft, and device condition photos are typically required. \
        This catalog entry exists so Ask Casa can retrieve grounded demo answers.
        """);
    product = productRepository.save(product);

    saveExclusion(
        product,
        "Pre-existing damage, wear and tear, battery degradation, and cosmetic scratches without "
            + "functional impact are not covered.");
    saveExclusion(
        product,
        "Loss without evidence of forced entry or a filed police report (for theft claims) is excluded.");
    saveExclusion(
        product,
        "Devices used primarily for commercial rental fleets or already covered under a manufacturer "
            + "warranty claim for the same event are excluded.");
    saveExclusion(
        product,
        "Intentional damage, confiscation by authorities, and damage from unapproved third-party repairs "
            + "are excluded.");

    saveFaq(
        product,
        "What devices can I cover?",
        "Eligible phones, laptops, and tablets within the declared value band (demo max ₦1,500,000). "
            + "Tablets and foldables may need specific tiers.");
    saveFaq(
        product,
        "How fast is repair or replacement?",
        "After approval, demo turnaround is typically 24–72 hours for common models, subject to parts "
            + "availability. Not a live SLA.");
    saveFaq(
        product,
        "Is there a deductible?",
        "Some demo tiers advertise low or no deductibles; exact excess amounts are plan-specific and "
            + "shown at quote time in a real product.");
    saveFaq(
        product,
        "Does cover include accessories?",
        "Standard demo cover focuses on the primary device. Chargers, cases, and third-party accessories "
            + "are generally excluded unless listed on the schedule.");

    return product;
  }

  private void saveExclusion(Product product, String text) {
    ProductExclusion exclusion = new ProductExclusion();
    exclusion.setProduct(product);
    exclusion.setText(text);
    exclusionRepository.save(exclusion);
  }

  private void saveFaq(Product product, String question, String answer) {
    ProductFaq faq = new ProductFaq();
    faq.setProduct(product);
    faq.setQuestion(question);
    faq.setAnswer(answer);
    faqRepository.save(faq);
  }
}
