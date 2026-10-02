import type { ProductFacts as ProductFactsType } from '../types/product'

interface ProductFactsProps {
  facts: ProductFactsType
}

const factLabels: Array<[keyof ProductFactsType, string]> = [
  ['brand', 'Brand'],
  ['model', 'Model'],
  ['color', 'Colour'],
  ['gradeLabel', 'Condition'],
  ['material', 'Material'],
  ['hardware', 'Hardware'],
  ['year', 'Year'],
  ['includes', 'Includes'],
  ['size', 'Size'],
  ['dimensions', 'Dimensions'],
  ['chainLength', 'Chain length'],
  ['strapLength', 'Strap length'],
  ['signsOfWear', 'Signs of wear'],
]

export function ProductFacts({ facts }: ProductFactsProps) {
  const visibleFacts = factLabels.filter(([key]) => facts[key]?.trim())

  if (visibleFacts.length === 0) {
    return (
      <p className="facts-empty">No additional product facts are available.</p>
    )
  }

  return (
    <dl className="facts-grid">
      {visibleFacts.map(([key, label]) => (
        <div className="fact-item" key={key}>
          <dt>{label}</dt>
          <dd>{facts[key]}</dd>
        </div>
      ))}
    </dl>
  )
}
